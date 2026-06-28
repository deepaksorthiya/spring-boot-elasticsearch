package com.example.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.document.comment.Comment;
import com.example.document.comment.CommentCreationDTO;
import com.example.document.comment.CommentForListDTO;
import com.example.document.comment.CommentsDTO;
import com.example.document.user.Author;
import com.example.document.user.User;
import com.example.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public Comment newComment(CommentCreationDTO commentDTO, String slug, User user) throws IOException {
        Author commentAuthor = new Author(user, false);
        Instant now = Instant.now();

        Long commentId = Long.valueOf(String.valueOf(new SecureRandom().nextLong()).substring(0, 15));
        Comment comment = new Comment(commentId, now, now, commentDTO.body(), commentAuthor,
                slug);

        commentRepository.save(comment);

        return comment;
    }

    public void deleteComment(String commentId, String username) throws IOException {
        commentRepository.deleteComment(commentId, username);
    }

    public CommentsDTO findAllCommentsByArticle(String slug, Optional<User> user) throws IOException {
        SearchResponse<Comment> commentsByArticle = commentRepository.findCommentsByArticleSlug(slug);

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
