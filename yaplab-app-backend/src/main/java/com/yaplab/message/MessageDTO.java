package com.yaplab.message;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Objects;

/**
 * A Data Transfer Object (DTO) for sending messages.
 * This DTO is used to encapsulate the data required to send a message,
 * including sender and receiver information, content, group ID, and file details.
 * @param senderId   ID of the user sending the message (required).
 * @param receiverId ID of the user receiving the message (optional).
 * @param content    Content of the message (optional).
 * @param groupId    ID of the group to which the message is sent (optional).
 * @param fileUrl    URL of the file attached to the message (optional).
 * @param fileName   Name of the file attached to the message (optional).
 * @param fileSize   Size of the file attached to the message in bytes (optional).
 * @param repliedToMessageId ID of the message replied to (optional).
 * @param edited boolean indicating if the message was edited
 * @param forwarded boolean indicating if the message was forwarded
 * @param editTimestamp timestamp of the last edit
 */
public record MessageDTO(
        @NotNull Long senderId,
        Long receiverId,
        String content,
        Long groupId,
        String fileUrl,
        String fileName,
        Long fileSize,
        Long repliedToMessageId,
        boolean edited,
        boolean forwarded,
        Instant editTimestamp
) {
    /**
     * Used to compare if two MessageDTO objects are equal.
     * Same message can have two different dto as objects. They reside at different memory locations.
     * Since both objects are same we compare fields of the objects to check equality.
     * @param o the reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     * If the obj argument is null or not an instance of MessageDTO, false is returned.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageDTO that = (MessageDTO) o;
        return edited == that.edited && forwarded == that.forwarded && Objects.equals(senderId, that.senderId) && Objects.equals(receiverId, that.receiverId) && Objects.equals(content, that.content) && Objects.equals(groupId, that.groupId) && Objects.equals(fileUrl, that.fileUrl) && Objects.equals(fileName, that.fileName) && Objects.equals(fileSize, that.fileSize) && Objects.equals(repliedToMessageId, that.repliedToMessageId) && Objects.equals(editTimestamp, that.editTimestamp);
    }

    /**
     * Generates a hash code for the MessageDTO object.
     * If two MessageDTO objects are equal, they will have the same hash code.
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(senderId, receiverId, content, groupId, fileUrl, fileName, fileSize, repliedToMessageId, edited, forwarded, editTimestamp);
    }

    /**
     * Returns a string representation of the MessageDTO object.
     * The string is in a human-readable format that includes all fields.
     * @return a string representation of the MessageDTO object.
     */
    @Override
    public String toString() {
        return "MessageDTO{" +
                "senderId=" + senderId +
                ", receiverId=" + receiverId +
                ", content='" + content + '\'' +
                ", groupId=" + groupId +
                ", fileUrl='" + fileUrl + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileSize=" + fileSize +
                ", repliedToMessageId=" + repliedToMessageId +
                ", edited=" + edited +
                ", forwarded=" + forwarded +
                ", editTimestamp=" + editTimestamp +
                '}';
    }
}