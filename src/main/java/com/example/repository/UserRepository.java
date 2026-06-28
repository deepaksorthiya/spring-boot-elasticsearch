package com.example.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.document.user.User;
import com.example.utils.UserIdPair;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

import static com.example.constant.Constants.USERS;
import static com.example.utils.Utility.extractId;
import static com.example.utils.Utility.extractSource;

@Repository
public class UserRepository {

    private final ElasticsearchClient esClient;

    public UserRepository(ElasticsearchClient esClient) {
        this.esClient = esClient;
    }

    public List<User> findByUsernameOrEmail(String username, String email) throws IOException {
        SearchResponse<User> checkUser = esClient.search(ss -> ss
                        .index(USERS.getName())
                        .query(q -> q
                                .bool(b -> b
                                        .should(s -> s
                                                .term(t -> t
                                                        .field("email.keyword")
                                                        .value(email))
                                        ).should(s -> s
                                                .term(t -> t
                                                        .field("username.keyword")
                                                        .value(username)))))
                , User.class);
        return checkUser.hits().hits().stream().map(Hit::source).toList();
    }

    public void save(User newUser) throws IOException {
        IndexRequest<User> userReq = IndexRequest.of((id -> id
                .index(USERS.getName())
                .refresh(Refresh.WaitFor)
                .document(newUser)));
        esClient.index(userReq);
    }

    public UserIdPair findUserByEmail(String email) throws IOException {
        SearchResponse<User> getUser = esClient.search(ss -> ss
                        .index(USERS.getName())
                        .query(q -> q
                                .term(t -> t
                                        .field("email.keyword")
                                        .value(email)))
                , User.class);
        if (getUser.hits().hits().isEmpty()) {
            return null;
        }
        return new UserIdPair(extractSource(getUser), extractId(getUser));
    }

    public UserIdPair findUserByToken(String token) throws IOException {
        SearchResponse<User> getUser = esClient.search(ss -> ss
                        .index(USERS.getName())
                        .query(q -> q
                                .term(t -> t
                                        .field("token.keyword")
                                        .value(token))
                        )
                , User.class);
        if (getUser.hits().hits().isEmpty()) {
            return null;
        }
        return new UserIdPair(extractSource(getUser), extractId(getUser));
    }

    public void updateUser(String id, User user) throws IOException {
        UpdateResponse<User> upUser = esClient.update(up -> up
                        .index(USERS.getName())
                        .id(id)
                        .refresh(Refresh.WaitFor)
                        .doc(user)
                , User.class);
        if (!upUser.result().name().equals("Updated")) {
            throw new RuntimeException("User update failed");
        }
    }

    public UserIdPair findUserByUsername(String username) throws IOException {
        SearchResponse<User> getUser = esClient.search(ss -> ss
                        .index(USERS.getName())
                        .query(q -> q
                                .term(t -> t
                                        .field("username.keyword")
                                        .value(username)))
                , User.class);
        if (getUser.hits().hits().isEmpty()) {
            return null;
        }
        return new UserIdPair(extractSource(getUser), extractId(getUser));
    }
}
