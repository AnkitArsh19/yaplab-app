package com.yaplab.message;

import com.yaplab.config.MessageEncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that automatically encrypts/decrypts the message content field.
 *
 * HOW IT WORKS:
 * - When saving a message to the database, convertToDatabaseColumn() is called.
 *   It encrypts the plaintext content before it hits the database.
 * - When loading a message from the database, convertToEntityAttribute() is called.
 *   It decrypts the content so your application always works with plaintext.
 *
 * This means:
 * - In the database: content is stored as encrypted gibberish (e.g., "ENC:a3F9x2...")
 * - In your Java code: content is always readable plaintext (e.g., "Hello!")
 * - No other code in the app needs to change. The encryption is invisible to
 *   MessageService, MessageMapper, MessageController, and the frontend.
 *
 * USAGE:
 * Applied via @Convert(converter = MessageContentConverter.class) on the
 * Message entity's content field. That one annotation is the only wiring needed.
 */
@Converter
public class MessageContentConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String content) {
        return MessageEncryptionUtil.encrypt(content);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return MessageEncryptionUtil.decrypt(dbData);
    }
}
