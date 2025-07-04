const AVATAR_COLORS = [
    '#745b4a',
    '#A8462E',
    '#304E50',
    '#C8866F',
    '#779292',
    '#D48669'
];

export const getAvatarColor = (name) => {
    if (!name) return AVATAR_COLORS[0];
    
    // Use the entire name for better color distribution
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        const char = name.charCodeAt(i);
        hash = ((hash << 5) - hash) + char;
        hash = hash & hash; // Convert to 32-bit integer
    }
    
    // Use absolute value and modulo to get a color index
    const colorIndex = Math.abs(hash) % AVATAR_COLORS.length;
    return AVATAR_COLORS[colorIndex];
};

/**
 * Get the first letter of a name in uppercase
 * @param {string} name - The user's name
 * @returns {string} - First letter in uppercase
 */
export const getInitials = (name) => {
    if (!name) return '?';
    return name.charAt(0).toUpperCase();
};

/**
 * Get the full URL for a profile picture or return null if not available
 * @param {string} profilePictureUrl - The relative URL from the backend
 * @returns {string|null} - Full URL or null if not available
 */
export const getProfilePictureUrl = (profilePictureUrl) => {
    if (!profilePictureUrl || typeof profilePictureUrl !== 'string') {
        return null;
    }
    
    // If it's a data URL, return as is
    if (profilePictureUrl.startsWith('data:')) {
        return profilePictureUrl;
    }
    
    // If it's already a full URL or a public asset path, use as is
    if (profilePictureUrl.startsWith('http') || profilePictureUrl.startsWith('/')) {
        return profilePictureUrl;
    }
    
    // If it starts with api/, prepend the base URL
    if (profilePictureUrl.startsWith('api/')) {
        return `${window.location.protocol}//${window.location.host}/${profilePictureUrl}`;
    }
    
    // For backward compatibility, assume it's a relative path
    return `${window.location.protocol}//${window.location.host}/api/${profilePictureUrl}`;
};

/**
 * Create a blob URL for a file to display as image preview
 * @param {File} file - The file object
 * @returns {string} - Blob URL
 */
export const createImagePreviewUrl = (file) => {
    return URL.createObjectURL(file);
};

/**
 * Clean up a blob URL to prevent memory leaks
 * @param {string} blobUrl - The blob URL to revoke
 */
export const revokeImagePreviewUrl = (blobUrl) => {
    if (blobUrl && blobUrl.startsWith('blob:')) {
        URL.revokeObjectURL(blobUrl);
    }
};