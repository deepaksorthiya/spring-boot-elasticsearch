/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.example.db;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.document.article.ArticleCreationDTO;
import com.example.document.comment.Comment;
import com.example.document.comment.CommentCreationDTO;
import com.example.document.comment.CommentForListDTO;
import com.example.document.comment.CommentsDTO;
import com.example.document.user.Author;
import com.example.document.user.RegisterDTO;
import com.example.document.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.example.constant.Constants.COMMENTS;

@Service
public class CommentService {

    private final ElasticsearchClient esClient;

    @Autowired
    public CommentService(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    /**
     * Creates a new comment and saves it into the comment index.
     * The refresh value is specified for the same reason explained in
     * {@link ArticleService#newArticle(ArticleCreationDTO, Author)}
     *
     * @return the newly created comment.
     */
    public Comment newComment(CommentCreationDTO commentDTO, String slug, User user) throws IOException {
        // assuming you cannot follow yourself
        Author commentAuthor = new Author(user, false);
        Instant now = Instant.now();

        // pre-generating id since it's a field in the comment class
        Long commentId = Long.valueOf(String.valueOf(new SecureRandom().nextLong()).substring(0, 15));
        Comment comment = new Comment(commentId, now, now, commentDTO.body(), commentAuthor,
                slug);

        IndexRequest<Comment> commentReq = IndexRequest.of((id -> id
                .index(COMMENTS.getName())
                .refresh(Refresh.WaitFor)
                .document(comment)));


        esClient.index(commentReq);

        return comment;
    }

    /**
     * Deletes a specific comment making sure that the user performing the operation is the author of the
     * comment itself.
     * <p>
     * A boolean query similar to the one used in {@link UserService#newUser(RegisterDTO)} is used,
     * matching both the comment id and the author's username, with a difference: here "must" is used
     * instead of "should", meaning that the documents must match both conditions at the same time.
     *
     * @return The authenticated user.
     */
    public void deleteComment(String commentId, String username) throws IOException {

        DeleteByQueryResponse deleteComment = esClient.deleteByQuery(ss -> ss
                .index(COMMENTS.getName())
                .waitForCompletion(true)
                .refresh(true)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .term(t -> t
                                                .field("id")
                                                .value(commentId))
                                ).must(m -> m
                                        .term(t -> t
                                                .field("author.username.keyword")
                                                .value(username))))
                ));
        if (deleteComment.deleted() < 1) {
            throw new RuntimeException("Failed to delete comment");
        }
    }

    /**
     * Retrieves all comments with the same articleSlug value using a term query
     * (see {@link UserService#findUserByUsername(String)}).
     *
     * @return a list of comment belonging to a single article.
     */
    public CommentsDTO findAllCommentsByArticle(String slug, Optional<User> user) throws IOException {
        SearchResponse<Comment> commentsByArticle = esClient.search(s -> s
                        .index(COMMENTS.getName())
                        .query(q -> q
                                .term(t -> t
                                        .field("articleSlug.keyword")
                                        .value(slug))
                        )
                , Comment.class);

        return new CommentsDTO(commentsByArticle.hits().hits().stream()
                .map(x -> new CommentForListDTO(x.source()))
                .map(c -> {
                    if (user.isPresent()) {
                        boolean following = user.get().following().contains(c.author().username());
                        return new CommentForListDTO(c.id(), c.createdAt(), c.updatedAt(), c.body(),
                                new Author(c.author().username(), c.author().email(), c.author().bio(),
                                        following));
                    }
                    return c;
                })
                .collect(Collectors.toList()));
    }
}
