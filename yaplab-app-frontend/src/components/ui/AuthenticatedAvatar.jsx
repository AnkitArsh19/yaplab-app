import React, { useState, useEffect } from 'react';
import { getAvatarColor, getInitials, getProfilePictureUrl } from '../../utils/avatarUtils.js';
import apiClient from '../../utils/apiClient.js';

function AuthenticatedAvatar({ 
    src, 
    name, 
    size = 45, 
    className = '', 
    style = {},
    onClick,
    fallbackToInitials = true
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
        ...style
    };

    useEffect(() => {
        const loadAuthenticatedImage = async () => {
            if (!fullProfilePictureUrl || fullProfilePictureUrl.includes('default-avatar.png')) {
                return;
            }
            
            setIsLoading(true);
            setHasError(false);

            try {
                const response = await apiClient.request(fullProfilePictureUrl, {
                    method: 'GET',
                    headers: {}
                });
                
                if (response.ok) {
                    const blob = await response.blob();
                    const url = URL.createObjectURL(blob);
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

        loadAuthenticatedImage();

        return () => {
            if (imageUrl) {
                URL.revokeObjectURL(imageUrl);
            }
        };
    }, [fullProfilePictureUrl]);

    // If we have a loaded image, show it
    if (imageUrl && !hasError) {
        return (
            <img 
                src={imageUrl} 
                alt={name} 
                className={className}
                style={avatarStyle}
                onClick={onClick}
                onError={() => {
                    setHasError(true);
                    if (imageUrl) {
                        URL.revokeObjectURL(imageUrl);
                        setImageUrl(null);
                    }
                }}
            />
        );
    }

    // If loading, show initials
    if (isLoading && fullProfilePictureUrl) {
        return (
            <div 
                className={className}
                style={{
                    ...avatarStyle,
                    opacity: 0.7
                }}
                onClick={onClick}
            >
                {initials}
            </div>
        );
    }

    // Fallback to initials
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

export default AuthenticatedAvatar;
