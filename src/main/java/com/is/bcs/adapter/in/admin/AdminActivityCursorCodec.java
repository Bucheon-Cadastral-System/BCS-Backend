package com.is.bcs.adapter.in.admin;

import com.is.bcs.adapter.in.web.exception.InvalidCursorException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

@Component
public class AdminActivityCursorCodec {

    public String encode(AdminActivityCursor cursor) {
        String value = cursor.createdAt() + "|" + cursor.id();

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public AdminActivityCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );

            String[] values = decoded.split("\\|", 2);

            if (values.length != 2) {
                throw new InvalidCursorException();
            }

            return new AdminActivityCursor(
                    OffsetDateTime.parse(values[0]),
                    Long.valueOf(values[1])
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidCursorException();
        }
    }
}