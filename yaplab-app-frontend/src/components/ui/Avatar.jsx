import React, { useState, useEffect } from 'react';
import { getAvatarColor, getInitials, getProfilePictureUrl } from '../../utils/avatarUtils.js';
import apiClient from '../../utils/apiClient.js';
import '../../styles/Avatar.css';

function Avatar({ 
    src, 
    name, 
    size = 45, 
    className = '', 
    style = {},
    onClick,
    isGroup = false 
}) {
    const [imageUrl, setImageUrl] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [hasError, setHasError] = useState(false);
    
    const avatarColor = getAvatarColor(name);
    const initials = getInitials(name);
    const fullProfilePictureUrl = getProfilePictureUrl(src);
    
    const avatarStyle = {
        width: `${size}px`,
        height: `${size}px`,
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: avatarColor,
        color: 'white',
        fontWeight: 'bold',
        fontSize: `${size * 0.4}px`,
        fontFamily: "'Regular', 'Roboto', Calibri, 'Trebuchet MS', sans-serif",
        textTransform: 'uppercase',
        userSelect: 'none',
        border: '2px solid rgba(255, 255, 255, 0.2)',
        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
        objectFit: 'cover',
        cursor: onClick ? 'pointer' : 'default',
        flexShrink: 0,
        position: 'relative',
        ...style
    };

    useEffect(() => {
        setImageUrl(null);
        setHasError(false);
        
        if (!fullProfilePictureUrl || fullProfilePictureUrl === 'null' || fullProfilePictureUrl === 'undefined') {
            return;
        }

        // Data URLs should be rendered directly without any fetching
        if (fullProfilePictureUrl.startsWith('data:image/')) {
            setImageUrl(fullProfilePictureUrl);
            return;
        }
        
        // Use authenticated loading for all backend-served files (/files/serve/)
        if (fullProfilePictureUrl.includes('/files/serve/')) {
            setIsLoading(true);
            let currentImageUrl = null;
            
            const loadImage = async () => {
                try {
                    // Extract just the path part for apiClient
                    const urlParts = fullProfilePictureUrl.split('/api');
                    const relativePath = urlParts[1] || fullProfilePictureUrl;
                    
                    const response = await apiClient.request(relativePath, {
                        method: 'GET',
                        headers: {}
                    });
                    
                    if (response.ok) {
                        const blob = await response.blob();
                        const url = URL.createObjectURL(blob);
                        currentImageUrl = url;
                        setImageUrl(url);
                    } else {
                        setHasError(true);
                    }
                } catch (error) {
                    setHasError(true);
                } finally {
                    setIsLoading(false);
                }
            };

            loadImage();

            return () => {
                if (currentImageUrl) {
                    URL.revokeObjectURL(currentImageUrl);
                }
            };
        }
    }, [fullProfilePictureUrl]);

    // If we're loading a backend image, show initials while loading
    if (isLoading && fullProfilePictureUrl && fullProfilePictureUrl.includes('/files/serve/')) {
        return (
            <div 
                className={className}
                style={avatarStyle}
                onClick={onClick}
            >
                {initials}
            </div>
        );
    }

    // If we have a loaded authenticated image or data URL, show it
    if (imageUrl) {
        return (
            <img 
                src={imageUrl} 
                alt={name} 
                className={className}
                style={avatarStyle}
                onClick={onClick}
            />
        );
    }

    // For external URLs (not backend-served and not data URLs), show them directly with error handling
    if (fullProfilePictureUrl && 
        !fullProfilePictureUrl.includes('/files/serve/') && 
        !fullProfilePictureUrl.startsWith('data:image/') && 
        !hasError) {
        return (
            <img 
                src={fullProfilePictureUrl} 
                alt={name} 
                className={className}
                style={avatarStyle}
                onClick={onClick}
                onError={() => {
                    setHasError(true);
                }}
            />
        );
    }

    return (
        <div className={`avatar-container ${className}`} style={{ position: 'relative', width: `${size}px`, height: `${size}px` }}>
            {imageUrl && !hasError ? (
                <img
                    src={imageUrl}
                    alt={name}
                    style={avatarStyle}
                    onClick={onClick}
                    onError={() => setHasError(true)}
                />
            ) : (
                <div style={avatarStyle} onClick={onClick}>
                    {initials}
                </div>
            )}
            {isGroup && (
                <div className="group-indicator">
                    <span className="group-icon">👥</span>
                </div>
            )}
        </div>
    );
}

export default Avatar;
