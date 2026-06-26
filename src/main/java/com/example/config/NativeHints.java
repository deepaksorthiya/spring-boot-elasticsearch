package com.example.config;

import com.example.document.article.*;
import com.example.document.comment.*;
import com.example.document.user.*;
import com.example.utils.UserIdPair;
import com.github.slugify.Slugify;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

public class NativeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        Class<?>[] classes = {
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

        for (Class<?> clazz : classes) {
            hints
                    .reflection()
                    .registerType(org.springframework.aot.hint.TypeReference.of(clazz), MemberCategory.values());
        }


        // Hints for io.jsonwebtoken (JJWT)
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
            hints
                    .reflection()
                    .registerType(org.springframework.aot.hint.TypeReference.of(clazz), MemberCategory.values());
        }

        // Hints for slugify
        hints.reflection().registerType(Slugify.class, MemberCategory.values());
    }
}
