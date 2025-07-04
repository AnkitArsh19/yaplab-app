import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from './Avatar';
import apiClient from '../../utils/apiClient';
import { getInitials, getAvatarColor } from '../../utils/avatarUtils';
import '../../styles/AboutComponent.css';

const AboutComponent = ({ 
    isOpen, 
    onClose, 
    data, 
    type = 'user', 
    mode = 'view', 
    currentUser, 
    members = [],
    title,
    onSave,
    onMemberClick
}) => {
    const [editedData, setEditedData] = useState(data || {});
    const [profilePicture, setProfilePicture] = useState(null);
    const [profilePicturePreview, setProfilePicturePreview] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [showMemberInfo, setShowMemberInfo] = useState(false);
    const [selectedMember, setSelectedMember] = useState(null);
    const [isEditingName, setIsEditingName] = useState(false);
    const fileInputRef = useRef(null);

    const isOwnProfile = type === 'user' && currentUser && data && currentUser.id === data.id;
    const isEditable = mode === 'edit' && isOwnProfile;

    const formatDate = (date) => {
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

    const getCreatorName = () => {
        if (type !== 'group') return 'Unknown';
        
        if (data.createdByName) return data.createdByName;
        if (data.group?.createdByName) return data.group.createdByName;
        
        const createdById = data.createdById || data.group?.createdById;
        if (createdById) {
            const creator = members.find(member => member.id === createdById);
            if (creator) return creator.userName || creator.name;
        }
        
        return 'Unknown';
    };

    const getCreatedAt = () => {
        if (type !== 'group') return null;
        return data.createdAt || data.group?.createdAt;
    };

    const handleClose = () => {
        setEditedData(data || {});
        setProfilePicture(null);
        setProfilePicturePreview(null);
        setShowMemberInfo(false);
        setSelectedMember(null);
        setIsEditingName(false);
        onClose();
    };

    const handleInputChange = (field, value) => {
        setEditedData(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const handleProfilePictureChange = (event) => {
        const file = event.target.files[0];
        if (file) {
            if (!file.type.startsWith('image/')) {
                alert('Please select an image file');
                return;
            }
            
            if (file.size > 5 * 1024 * 1024) {
                alert('File size must be less than 5MB');
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
        if (!isEditable) return;
        
        setIsLoading(true);
        try {
            if (profilePicture) {
                try {
                    const uploadResponse = await apiClient.uploadUserProfilePicture(currentUser.id, profilePicture);
                    if (!uploadResponse.ok) {
                        const errorText = await uploadResponse.text();
                        console.error('Profile picture upload failed:', uploadResponse.status, errorText);
                        throw new Error(`Failed to upload profile picture: ${uploadResponse.status} - ${errorText}`);
                    }
                } catch (uploadError) {
                    console.error('Profile picture upload error:', uploadError);
                    throw new Error(`Profile picture upload failed: ${uploadError.message}`);
                }
            }

            const currentName = editedData.userName || editedData.name;
            const originalName = data.userName || data.name;
            
            if (currentName && currentName !== originalName) {
                const userUpdatePayload = {
                    id: currentUser.id,
                    userName: currentName,
                    emailId: editedData.emailId || editedData.email || currentUser.emailId,
                    mobileNumber: editedData.mobileNumber || editedData.phoneNumber || currentUser.mobileNumber,
                    password: 'dummy123456'
                };

                const updateResponse = await apiClient.put('/users/update', userUpdatePayload);
                if (!updateResponse.ok) {
                    const errorText = await updateResponse.text();
                    console.error('User update failed:', updateResponse.status, errorText);
                    throw new Error(`Failed to update user information: ${updateResponse.status} - ${errorText}`);
                }
            }

            const userResponse = await apiClient.get(`/users/${currentUser.id}`);
            if (userResponse.ok) {
                const updatedUser = await userResponse.json();
                setEditedData(updatedUser);
                
                if (onSave) {
                    try {
                        await onSave(updatedUser);
                    } catch (error) {
                        console.warn('Parent onSave callback failed, but changes were saved:', error);
                    }
                }
            }
            
            handleClose();
        } catch (error) {
            console.error('Error saving data:', error);
            alert(`Failed to save changes: ${error.message}`);
        } finally {
            setIsLoading(false);
        }
    };

    const handleMemberClick = (member) => {
        if (onMemberClick) {
            onMemberClick(member);
        } else {
            setSelectedMember(member);
            setShowMemberInfo(true);
        }
    };

    const handleCloseMemberInfo = () => {
        setShowMemberInfo(false);
        setSelectedMember(null);
    };

    const getModalTitle = () => {
        if (title) return title;
        
        if (type === 'group') {
            return 'Group Info';
        }
        
        if (isOwnProfile && mode === 'edit') {
            return 'Edit Profile';
        }
        
        if (isOwnProfile) {
            return 'About';
        }
        
        return data?.userName || data?.name ? `${data.userName || data.name}'s Info` : 'User Info';
    };

    useEffect(() => {
        const fetchCompleteUserData = async () => {
            if (type === 'user' && data && data.id) {
                try {
                    const response = await apiClient.get(`/users/${data.id}`);
                    if (response.ok) {
                        const completeUserData = await response.json();
                        setEditedData(prev => ({
                            ...prev,
                            ...completeUserData
                        }));
                    }
                } catch (error) {
                    console.error('Error fetching complete user data:', error);
                }
            }
        };

        if (isOpen) {
            fetchCompleteUserData();
        }
    }, [isOpen, type, data]);

    if (!isOpen || !data) return null;

    const currentProfilePicture = profilePicturePreview || 
                                editedData.profilePictureUrl || 
                                data.profilePictureUrl || 
                                data.profilePicture || 
                                data.avatarUrl;
    const displayName = editedData.userName || data.userName || data.name;

    return (
        <>
            <AnimatePresence>
                <motion.div 
                    className="about-overlay"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={handleClose}
                >
                    <motion.div 
                        className="about-modal"
                        initial={{ opacity: 0, scale: 0.9, y: -20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.9, y: -20 }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="about-header">
                            <h3 className="about-title">{getModalTitle()}</h3>
                            <button className="about-close-btn" onClick={handleClose}>
                                <img src="/cross-icon.png" alt="Close" />
                            </button>
                        </div>

                        <div className="about-profile-section">
                            <div className="about-picture-container">
                                <Avatar
                                    src={currentProfilePicture}
                                    name={data.userName || data.name}
                                    size={120}
                                    className="about-avatar"
                                />
                                
                                {isEditable && (
                                    <button 
                                        className="about-change-picture-btn"
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

                            {type === 'user' && (
                                <div className="about-name-section">
                                    {isEditable && isEditingName ? (
                                        <div className="about-name-input-container">
                                            <input
                                                type="text"
                                                className="about-edit-name-input"
                                                value={editedData.userName || editedData.name || ''}
                                                onChange={(e) => handleInputChange('userName', e.target.value)}
                                                onBlur={() => setIsEditingName(false)}
                                                onKeyDown={(e) => {
                                                    if (e.key === 'Enter') {
                                                        setIsEditingName(false);
                                                    }
                                                }}
                                                placeholder="Enter your name"
                                                autoFocus
                                            />
                                        </div>
                                    ) : (
                                        <div className="about-name-display-container">
                                            <h2 
                                                className="about-user-name"
                                                onClick={() => isEditable && setIsEditingName(true)}
                                                style={{ 
                                                    cursor: isEditable ? 'pointer' : 'default'
                                                }}
                                                title={isEditable ? 'Click to edit name' : ''}
                                            >
                                                {displayName || 'Not provided'}
                                            </h2>
                                            {isEditable && (
                                                <span className="about-edit-hint">Click to edit</span>
                                            )}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        <div className="about-content">
                            {type === 'user' ? (
                                <div className="about-info-section">
                                    <div className="about-info-item">
                                        <p className="about-info-label">Email</p>
                                        <p className={`about-info-value ${!editedData.emailId && !editedData.email ? 'not-provided' : ''}`}>
                                            {editedData.emailId || editedData.email || 'Not provided'}
                                        </p>
                                    </div>

                                    <div className="about-info-item">
                                        <p className="about-info-label">Phone Number</p>
                                        <p className={`about-info-value ${!editedData.mobileNumber && !editedData.phoneNumber ? 'not-provided' : ''}`}>
                                            {editedData.mobileNumber || editedData.phoneNumber || 'Not provided'}
                                        </p>
                                    </div>

                                    <div className="about-info-item">
                                        <p className="about-info-label">
                                            {isOwnProfile ? 'Member since' : 'Joined'}
                                        </p>
                                        <p className="about-info-value">
                                            {formatDate(editedData.createdAt || editedData.joinedAt)}
                                        </p>
                                    </div>
                                </div>
                            ) : (
                                <div className="about-group-section">
                                    <div className="about-group-header">
                                        <h2 className="about-group-name">
                                            {data.name || 'Unnamed Group'}
                                        </h2>
                                        {data.description && (
                                            <p className="about-group-description">
                                                {data.description}
                                            </p>
                                        )}
                                    </div>
                                    
                                    <div className="about-info-item">
                                        <p className="about-info-label">Created By</p>
                                        <p className="about-info-value">
                                            {data.createdByName || getCreatorName()}
                                        </p>
                                    </div>

                                    <div className="about-info-item">
                                        <p className="about-info-label">Created On</p>
                                        <p className="about-info-value">
                                            {formatDate(data.createdAt)}
                                        </p>
                                    </div>
                                </div>
                            )}
                        </div>

                        {type === 'group' && members.length > 0 && (
                            <div className="about-members-section">
                                <div className="about-members-header">
                                    <h4 className="about-members-title">Members</h4>
                                    <span className="about-members-count">
                                        {members.length} member{members.length !== 1 ? 's' : ''}
                                    </span>
                                </div>
                                <div className="about-members-list">
                                    {members.map((member, index) => {
                                        const isCreator = member.id === data.createdById;
                                        
                                        return (
                                            <div 
                                                key={member.id || index} 
                                                className="about-member-item"
                                                onClick={() => handleMemberClick(member)}
                                            >
                                                <div className="about-member-avatar">
                                                    <Avatar
                                                        src={member.profilePictureUrl}
                                                        name={member.userName || member.name}
                                                        size={48}
                                                    />
                                                </div>
                                                <div className="about-member-info">
                                                    <div className="about-member-name">
                                                        {member.userName || member.name || 'Unknown'}
                                                        {isCreator && <span className="about-creator-badge">Admin</span>}
                                                    </div>
                                                    <div className="about-member-details">
                                                        {member.emailId && (
                                                            <p className="about-member-email">
                                                                {member.emailId}
                                                            </p>
                                                        )}
                                                        {member.mobileNumber && (
                                                            <p className="about-member-phone">
                                                                {member.mobileNumber}
                                                            </p>
                                                        )}
                                                    </div>
                                                </div>
                                                <div className="about-member-arrow">
                                                    <img src="/chevron-down-solid.svg" alt="View" />
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        )}

                        {isEditable && (
                            <div className="about-footer">
                                <button 
                                    className={`about-save-btn ${isLoading ? 'loading' : ''}`}
                                    onClick={handleSave}
                                    disabled={isLoading}
                                >
                                    {isLoading ? 'Saving...' : 'Save Changes'}
                                </button>
                            </div>
                        )}
                    </motion.div>
                </motion.div>
            </AnimatePresence>

            {showMemberInfo && selectedMember && (
                <div style={{ zIndex: 10001 }}>
                    <AboutComponent
                        isOpen={showMemberInfo}
                        onClose={handleCloseMemberInfo}
                        data={selectedMember}
                        type="user"
                        mode="view"
                        currentUser={currentUser}
                        title={`${selectedMember.userName || selectedMember.name}'s Info`}
                    />
                </div>
            )}
        </>
    );
};

export default AboutComponent;
