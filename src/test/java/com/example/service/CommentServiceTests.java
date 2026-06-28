package com.example.service;

import com.example.document.comment.Comment;
import com.example.document.comment.CommentCreationDTO;
import com.example.document.user.User;
import com.example.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTests {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void shouldCreateCommentSuccessfully() throws IOException {
        User user = new User("commentuser", "comment@test.com", "pw", "token", "bio", "image", new byte[0], new ArrayList<>());
        CommentCreationDTO commentDTO = new CommentCreationDTO("This is a test comment");

        Comment created = commentService.newComment(commentDTO, "comment-test-article", user);

        assertThat(created).isNotNull();
        assertThat(created.body()).isEqualTo("This is a test comment");
        assertThat(created.author().username()).isEqualTo("commentuser");

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void shouldDeleteCommentSuccessfully() throws IOException {
        commentService.deleteComment("12345", "deletecommentuser");
        verify(commentRepository).deleteComment("12345", "deletecommentuser");
    }
}
