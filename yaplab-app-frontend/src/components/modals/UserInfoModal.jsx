import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../utils/apiClient';
import Notification from '../ui/Notification';
import Avatar from '../ui/Avatar';
import '../../styles/UserInfoModal.css';

const UserInfoModal = ({ 
    isOpen, 
    onClose, 
    user, 
    mode = 'view', // 'view', 'edit', 'about'
    title,
    onSave 
}) => {
    const [editedUser, setEditedUser] = useState(user || {});
    const [completeUserData, setCompleteUserData] = useState(null);
    const [profilePicture, setProfilePicture] = useState(null);
    const [profilePicturePreview, setProfilePicturePreview] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [notification, setNotification] = useState({ message: '', type: '' });
    const fileInputRef = useRef(null);

    const isEditable = mode === 'edit' || mode === 'about';

    const showNotification = (message, type = 'info') => {
        setNotification({ message, type });
    };

    const formatJoiningDate = (date) => {
        if (!date) return 'Unknown';
        try {
            return new Date(date).toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
            });
        } catch (error) {
            return 'Unknown';
        }
    };

    useEffect(() => {
        const fetchCompleteUserData = async () => {
            if (isOpen && user?.id && !completeUserData) {
                try {
                    setIsLoading(true);
                    const response = await apiClient.get(`/users/${user.id}`);
                    if (response.ok) {
                        const userData = await response.json();
                        setCompleteUserData(userData);
                        setEditedUser(userData);
                    }
                } catch (error) {
                    console.error('Error fetching complete user data:', error);
                    setCompleteUserData(user);
                    setEditedUser(user);
                } finally {
                    setIsLoading(false);
                }
            }
        };

        fetchCompleteUserData();
    }, [isOpen, user?.id]);

    useEffect(() => {
        if (!isOpen) {
            setCompleteUserData(null);
            setEditedUser(user || {});
            setProfilePicture(null);
            setProfilePicturePreview(null);
        }
    }, [isOpen, user]);

    const handleClose = () => {
        setEditedUser(completeUserData || user || {});
        setProfilePicture(null);
        setProfilePicturePreview(null);
        onClose();
    };

    const handleInputChange = (field, value) => {
        setEditedUser(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const handleProfilePictureChange = (event) => {
        const file = event.target.files[0];
        if (file) {
            if (!file.type.startsWith('image/')) {
                return;
            }
            
            if (file.size > 5 * 1024 * 1024) {
                showNotification('Profile picture must be less than 5MB', 'error');
                return;
            }

            setProfilePicture(file);
            
            const reader = new FileReader();
            reader.onload = (e) => {
                setProfilePicturePreview(e.target.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSave = async () => {
        if (!isEditable || !onSave) return;
        
        setIsLoading(true);
        try {
            if (profilePicture) {
                const formData = new FormData();
                formData.append('file', profilePicture);
                
                const uploadResponse = await apiClient.request(`/users/${editedUser.id}/profile-picture`, {
                    method: 'POST',
                    body: formData
                });
                
                if (!uploadResponse.ok) {
                    showNotification('Failed to upload profile picture', 'error');
                    setIsLoading(false);
                    return;
                }
            }
            
            await onSave(editedUser);
            
            showNotification('Profile updated successfully!', 'success');
            handleClose();
            
        } catch (error) {
            console.error('Error saving user info:', error);
            showNotification('Failed to save changes. Please try again.', 'error');
        } finally {
            setIsLoading(false);
        }
    };

    const getModalTitle = () => {
        if (title) return title;
        switch (mode) {
            case 'edit':
                return 'Edit Profile';
            case 'about':
                return 'Edit Profile';
            case 'view':
                return displayUser?.userName ? `${displayUser.userName}'s Info` : 'User Info';
            default:
                return 'User Information';
        }
    };

    if (!isOpen || !user) return null;

    if (isLoading && !completeUserData) {
        return (
            <AnimatePresence>
                <motion.div 
                    key="user-info-loading-overlay"
                    className="user-info-overlay"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={handleClose}
                >
                    <motion.div 
                        key="user-info-loading-modal"
                        className="user-info-modal"
                        initial={{ opacity: 0, scale: 0.9, y: -20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.9, y: -20 }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="user-info-header">
                            <h3 className="user-info-title">Loading...</h3>
                            <button className="user-info-close-btn" onClick={handleClose}>
                                <img src="/cross-icon.png" alt="Close" />
                            </button>
                        </div>
                        <div className="user-info-content" style={{ textAlign: 'center', padding: '60px 24px' }}>
                            <p>Loading user information...</p>
                        </div>
                    </motion.div>
                </motion.div>
            </AnimatePresence>
        );
    }

    const displayUser = completeUserData || user;
    const currentProfilePicture = profilePicturePreview || editedUser.profilePictureUrl || displayUser.profilePictureUrl;

    return (
        <AnimatePresence>
            <motion.div 
                key="user-info-overlay"
                className="user-info-overlay"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={handleClose}
            >
                <motion.div 
                    key="user-info-modal"
                    className="user-info-modal"
                    initial={{ opacity: 0, scale: 0.9, y: -20 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.9, y: -20 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="user-info-header">
                        <h3 className="user-info-title">{getModalTitle()}</h3>
                        <button className="user-info-close-btn" onClick={handleClose}>
                            <img src="/cross-icon.png" alt="Close" />
                        </button>
                    </div>

                    <div className="user-info-profile-section">
                        <div className="user-info-picture-container">
                            <Avatar
                                src={currentProfilePicture}
                                name={displayUser.userName || displayUser.name}
                                size={100}
                                className="user-info-avatar"
                            />
                            
                            {isEditable && (
                                <button 
                                    className="user-info-change-picture-btn"
                                    onClick={() => fileInputRef.current?.click()}
                                    title="Change profile picture"
                                >
                                    <img src="/pen-solid.svg" alt="Edit" />
                                </button>
                            )}
                            
                            {isEditable && (
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept="image/*"
                                    onChange={handleProfilePictureChange}
                                    style={{ display: 'none' }}
                                />
                            )}
                        </div>
                    </div>

                    <div className="user-info-content">
                        <div className="user-info-name-section">
                            {isEditable ? (
                                <div className="user-info-name-display-container">
                                    <div className="user-info-name-editable">
                                        <input
                                            type="text"
                                            className="user-info-name-input"
                                            value={editedUser.userName || editedUser.name || ''}
                                            onChange={(e) => handleInputChange('userName', e.target.value)}
                                            placeholder="Enter your name"
                                        />
                                    </div>
                                    <span className="user-info-click-to-edit">Click to edit</span>
                                </div>
                            ) : (
                                <div className="user-info-name-display-container">
                                    <div className="user-info-name-display">
                                        {displayUser.userName || displayUser.name || 'Not provided'}
                                    </div>
                                    <span className="user-info-click-to-edit">Click to edit</span>
                                </div>
                            )}
                        </div>

                        <div className="user-info-fields-container">
                            <div className="user-info-field">
                                <label className="user-info-label">EMAIL</label>
                                <div className="user-info-value readonly">
                                    {displayUser.emailId || displayUser.email || 'Not provided'}
                                </div>
                            </div>

                            <div className="user-info-field">
                                <label className="user-info-label">PHONE NUMBER</label>
                                <div className="user-info-value readonly">
                                    {displayUser.mobileNumber || displayUser.phoneNumber || 'Not provided'}
                                </div>
                            </div>

                            <div className="user-info-field">
                                <label className="user-info-label">MEMBER SINCE</label>
                                <div className="user-info-value readonly">
                                    {formatJoiningDate(displayUser.createdAt || displayUser.joinedAt)}
                                </div>
                            </div>
                        </div>
                    </div>

                    {isEditable && (
                        <div className="user-info-footer">
                            <button 
                                className={`user-info-save-btn ${isLoading ? 'loading' : ''}`}
                                onClick={handleSave}
                                disabled={isLoading}
                            >
                                {isLoading ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>
                    )}
                </motion.div>
            </motion.div>

            <Notification
                message={notification.message}
                type={notification.type}
                onClose={() => setNotification({ message: '', type: '' })}
            />
        </AnimatePresence>
    );
};

export default UserInfoModal;
