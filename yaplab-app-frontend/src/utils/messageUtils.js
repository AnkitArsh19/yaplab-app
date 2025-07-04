/**
 * Utility functions for message formatting
 */

/**
 * Format a message for display in chat lists/sidebar
 * @param {Object} message - The message object
 * @param {Object} currentUser - The current user object
 * @param {Object} chatRoom - The chat room object (contains participants and type info)
 * @returns {string} - Formatted message text
 */
export const formatMessageForList = (message, currentUser = null, chatRoom = null) => {
    if (!message) return "No messages yet";
    
    const isGroupChat = chatRoom?.chatRoomType === 'GROUP';
    const isOwnMessage = currentUser && message.senderId === currentUser.id;
    
    // Get sender prefix for group chats
    const getSenderPrefix = () => {
        if (!isGroupChat) return '';
        
        if (isOwnMessage) {
            return 'You: ';
        } else {
            // Find sender name from message or participants
            const senderName = message.senderName || 
                              chatRoom?.participants?.find(p => p.id === message.senderId)?.userName || 
                              'Unknown';
            return `${senderName}: `;
        }
    };
    
    const senderPrefix = getSenderPrefix();
    
    // Handle file messages (both regular and forwarded)
    if (message.fileUrl) {
        if (message.fileType === 'image/gif') {
            return `${senderPrefix}🎬 GIF`;
        } else if (message.fileType?.startsWith('image/')) {
            return `${senderPrefix}📷 Photo`;
        } else if (message.fileType?.startsWith('video/')) {
            return `${senderPrefix}🎥 Video`;
        } else if (message.fileType?.startsWith('audio/')) {
            return `${senderPrefix}🎵 Audio`;
        } else {
            return `${senderPrefix}📎 File`;
        }
    }
    
    // Handle text messages (both regular and forwarded)
    const messageContent = message.content || "No messages yet";
    return `${senderPrefix}${messageContent}`;
};
