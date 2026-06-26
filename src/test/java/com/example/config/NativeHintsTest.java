package com.example.config;

import com.example.document.article.*;
import com.example.document.comment.*;
import com.example.document.user.*;
import com.example.utils.UserIdPair;
import com.github.slugify.Slugify;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;

class NativeHintsTest {

    @Test
    void shouldRegisterHints() {
        RuntimeHints hints = new RuntimeHints();
        NativeHints nativeHints = new NativeHints();
        nativeHints.registerHints(hints, getClass().getClassLoader());

        Class<?>[] classesToCheck = {
                // Article models
                Article.class,
                ArticleCreationDTO.class,
                ArticleDTO.class,
                ArticleForListDTO.class,
                ArticleUpdateDTO.class,
                ArticlesDTO.class,
                TagsDTO.class,

                // Comment models
                Comment.class,
                CommentCreationDTO.class,
                CommentDTO.class,
                CommentForListDTO.class,
                CommentsDTO.class,

                // User models
                Author.class,
                LoginDTO.class,
                Profile.class,
                RegisterDTO.class,
                User.class,
                UserDTO.class,

                // Utility models
                UserIdPair.class
        };

        for (Class<?> clazz : classesToCheck) {
            // Verify Reflection Hints
            assertThat(RuntimeHintsPredicates.reflection().onType(clazz)
                    .withMemberCategories(MemberCategory.values())).accepts(hints);
        }

        // Verify JJWT Hints
        String[] jjwtClasses = {
                "io.jsonwebtoken.impl.security.KeysBridge",
                "io.jsonwebtoken.impl.security.StandardEncryptionAlgorithms",
                "io.jsonwebtoken.impl.security.StandardKeyAlgorithms",
                "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms",
                "io.jsonwebtoken.impl.io.StandardCompressionAlgorithms",
                "io.jsonwebtoken.impl.security.StandardKeyOperations",
                "io.jsonwebtoken.impl.security.JwksBridge",
                "io.jsonwebtoken.impl.DefaultJwtBuilder$Supplier",
                "io.jsonwebtoken.impl.DefaultJwtParserBuilder$Supplier",
                "io.jsonwebtoken.impl.DefaultJwtHeaderBuilder$Supplier",
                "io.jsonwebtoken.impl.DefaultClaimsBuilder$Supplier",
                "io.jsonwebtoken.impl.security.DefaultDynamicJwkBuilder$Supplier",
                "io.jsonwebtoken.impl.security.DefaultJwkParserBuilder$Supplier",
                "io.jsonwebtoken.impl.security.DefaultJwkSetBuilder$Supplier",
                "io.jsonwebtoken.impl.security.DefaultJwkSetParserBuilder$Supplier",
                "io.jsonwebtoken.impl.security.DefaultKeyOperationBuilder$Supplier",
                "io.jsonwebtoken.impl.security.DefaultKeyOperationPolicyBuilder$Supplier"
        };

        for (String clazz : jjwtClasses) {
            assertThat(RuntimeHintsPredicates.reflection().onType(TypeReference.of(clazz))
                    .withMemberCategories(MemberCategory.values())).accepts(hints);
        }

        // Verify Slugify Hints
        assertThat(RuntimeHintsPredicates.reflection().onType(Slugify.class)
                .withMemberCategories(MemberCategory.values())).accepts(hints);
    }
}
