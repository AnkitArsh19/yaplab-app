import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import data from '@emoji-mart/data';
import Picker from '@emoji-mart/react';
import { getAvatarColor, getInitials, createImagePreviewUrl, revokeImagePreviewUrl } from '../../utils/avatarUtils';
import '../../styles/GroupDetailsModal.css';

function GroupDetailsModal({ 
    isOpen, 
    onClose, 
    selectedContacts = [], 
    currentUser, 
    onCreateGroup 
}) {
    const [groupName, setGroupName] = useState('');
    const [profilePicture, setProfilePicture] = useState(null);
    const [profilePicturePreview, setProfilePicturePreview] = useState(null);
    const [isCreating, setIsCreating] = useState(false);
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const fileInputRef = useRef(null);
    const groupNameInputRef = useRef(null);
    const emojiPickerRef = useRef(null);

    const getDefaultProfilePicture = (name) => ({
        initial: getInitials(name || 'Group'),
        color: getAvatarColor(name || 'Group'),
        textColor: '#FFFFFF'
    });

    useEffect(() => {
        if (isOpen && groupNameInputRef.current) {
            setTimeout(() => {
                groupNameInputRef.current.focus();
            }, 100);
        }
    }, [isOpen]);
    
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (emojiPickerRef.current && !emojiPickerRef.current.contains(event.target)) {
                setShowEmojiPicker(false);
            }
        };

        if (showEmojiPicker) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen, showEmojiPicker]);

    const handleClose = () => {
        if (profilePicturePreview?.startsWith('blob:')) {
            revokeImagePreviewUrl(profilePicturePreview);
        }
        
        setGroupName('');
        setProfilePicture(null);
        setProfilePicturePreview(null);
        setIsCreating(false);
        setShowEmojiPicker(false);
        onClose();
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

            if (profilePicturePreview?.startsWith('blob:')) {
                revokeImagePreviewUrl(profilePicturePreview);
            }

            setProfilePicture(file);
            setProfilePicturePreview(createImagePreviewUrl(file));
        }
    };

    const handleRemoveProfilePicture = () => {
        if (profilePicturePreview?.startsWith('blob:')) {
            revokeImagePreviewUrl(profilePicturePreview);
        }
        
        setProfilePicture(null);
        setProfilePicturePreview(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleEmojiSelect = (emoji) => {
        setGroupName(prev => prev + emoji.native);
        setShowEmojiPicker(false);
        groupNameInputRef.current?.focus();
    };

    const handleCreateGroup = async () => {
        if (!groupName.trim()) {
            alert('Please enter a group name');
            return;
        }

        if (selectedContacts.length === 0) {
            alert('Please select at least one contact');
            return;
        }

        setIsCreating(true);
        
        try {
            const userIds = selectedContacts.map(contact => {
                if (contact.participants) {
                    const otherParticipant = contact.participants.find(p => p.id !== currentUser?.id);
                    return otherParticipant?.id;
                }
                return contact.userId || contact.id;
            }).filter(Boolean);

            if (currentUser?.id) {
                userIds.push(currentUser.id);
            }

            await onCreateGroup({
                name: groupName.trim(),
                userIds,
                profilePicture
            });

            handleClose();
        } catch (error) {
            alert('Failed to create group. Please try again.');
        } finally {
            setIsCreating(false);
        }
    };

    const getContactName = (contact) => {
        if (contact.participants) {
            const otherParticipant = contact.participants.find(p => p.id !== currentUser?.id);
            return otherParticipant?.userName || 'Unknown';
        }
        return contact.userName || contact.name || 'Unknown';
    };

    const getContactAvatar = (contact) => {
        if (contact.participants) {
            const otherParticipant = contact.participants.find(p => p.id !== currentUser?.id);
            return otherParticipant?.profilePictureUrl;
        }
        return contact.profilePictureUrl || contact.avatar;
    };

    const defaultProfile = getDefaultProfilePicture(groupName);

    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div 
                    className="group-details-overlay"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={handleClose}
                >
                <motion.div 
                    className="group-details-modal"
                    initial={{ opacity: 0, scale: 0.9, y: -20 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.9, y: -20 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="group-details-header">
                        <button className="group-details-close-btn" onClick={handleClose}>
                            <img src="cross-icon.png" alt="Close" />
                        </button>
                        <div className="group-details-header-info">
                            <h3 className="group-details-title">New Group</h3>
                            <p className="group-details-subtitle">
                                {selectedContacts.length} member{selectedContacts.length !== 1 ? 's' : ''} selected
                            </p>
                        </div>
                    </div>

                    <div className="group-profile-section">
                        <div className="group-profile-picture-container">
                            {profilePicturePreview ? (
                                <div className="group-profile-picture-wrapper">
                                    <img 
                                        src={profilePicturePreview} 
                                        alt="Group profile" 
                                        className="group-profile-picture-preview"
                                    />
                                </div>
                            ) : (
                                <div 
                                    className="group-default-picture"
                                    style={{ backgroundColor: defaultProfile.color }}
                                >
                                    <span style={{ color: defaultProfile.textColor }}>
                                        {defaultProfile.initial}
                                    </span>
                                </div>
                            )}
                            
                            <button 
                                className="group-change-picture-btn"
                                onClick={() => fileInputRef.current?.click()}
                                title="Change group picture"
                            >
                                <img src="/pen-solid.svg" alt="Edit" />
                            </button>
                            
                            {profilePicturePreview && (
                                <button
                                    className="group-remove-picture-btn"
                                    onClick={handleRemoveProfilePicture}
                                    title="Remove picture"
                                >
                                    <img src="/delete-icon.svg" alt="Remove" />
                                </button>
                            )}
                            
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept="image/*"
                                onChange={handleProfilePictureChange}
                                style={{ display: 'none' }}
                            />
                        </div>

                        <div className="group-name-section">
                            <div className="caption-input-container">
                                <button 
                                    className="emoji-btn"
                                    onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                                    title="Add emoji"
                                    type="button"
                                >
                                    <img src="/emoji-icon.svg" alt="Emoji" />
                                </button>
                                
                                <input
                                    ref={groupNameInputRef}
                                    type="text"
                                    placeholder="Group name"
                                    value={groupName}
                                    onChange={(e) => setGroupName(e.target.value)}
                                    className="caption-input"
                                    maxLength={50}
                                />
                            </div>
                            
                            {showEmojiPicker && (
                                <div className="emoji-picker-container" ref={emojiPickerRef}>
                                    <Picker
                                        data={data}
                                        onEmojiSelect={handleEmojiSelect}
                                        theme="light"
                                        previewPosition="none"
                                        skinTonePosition="none"
                                        set="native"
                                    />
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="selected-contacts-section">
                        <h4 className="selected-contacts-title">Selected Contacts</h4>
                        <div className="selected-contacts-list">
                            {selectedContacts.map((contact, index) => {
                                const contactName = getContactName(contact);
                                const contactAvatar = getContactAvatar(contact);
                                
                                return (
                                    <div key={`contact-${contact.id || index}`} className="selected-contact-item">
                                        <div className="selected-contact-avatar">
                                            {contactAvatar ? (
                                                <img src={contactAvatar} alt={contactName} />
                                            ) : (
                                                <div className="selected-contact-avatar-default">
                                                    {contactName.charAt(0).toUpperCase()}
                                                </div>
                                            )}
                                        </div>
                                        <span className="selected-contact-name">{contactName}</span>
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    <div className="group-details-footer">
                        <button 
                            className="group-details-cancel-btn" 
                            onClick={handleClose}
                            disabled={isCreating}
                        >
                            Cancel
                        </button>
                        <button 
                            className={`group-details-create-btn ${!groupName.trim() || isCreating ? 'disabled' : ''}`}
                            onClick={handleCreateGroup}
                            disabled={!groupName.trim() || isCreating}
                        >
                            {isCreating ? 'Creating...' : 'Create Group'}
                        </button>
                    </div>
                </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}

export default GroupDetailsModal;
