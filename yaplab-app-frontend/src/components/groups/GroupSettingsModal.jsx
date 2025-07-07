import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from '../ui/Avatar';
import ContactSelectionModal from '../modals/ContactSelectionModal';
import ConfirmationModal from '../modals/ConfirmationModal';
import apiClient from '../../utils/apiClient';
import '../../styles/GroupSettingsModal.css';

const GroupSettingsModal = ({
    isOpen,
    onClose,
    group,
    members = [],
    currentUser,
    chats = [], // Available chats/users for adding members
    onUpdateGroup,
    onAddMembers,
    onRemoveMember,
    onDeleteGroup
}) => {
    const [groupName, setGroupName] = useState(group?.name || '');
    const [isEditingName, setIsEditingName] = useState(false);
    const [selectedAvatar, setSelectedAvatar] = useState(null);
    const [avatarPreview, setAvatarPreview] = useState(null);
    const [showAddMembersModal, setShowAddMembersModal] = useState(false);
    const [availableUsers, setAvailableUsers] = useState([]);
    const [loadingUsers, setLoadingUsers] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [memberToRemove, setMemberToRemove] = useState(null);
    const [showRemoveMemberConfirm, setShowRemoveMemberConfirm] = useState(false);

    const avatarInputRef = useRef(null);

    // Reset form data when modal opens/closes or group changes
    useEffect(() => {
        setGroupName(group?.name || '');
        setAvatarPreview(null);
        setSelectedAvatar(null);
        setError(null);
    }, [group, isOpen]);

    useEffect(() => {
        return () => {
            if (avatarPreview && avatarPreview.startsWith('blob:')) {
                URL.revokeObjectURL(avatarPreview);
            }
        };
    }, [avatarPreview]);

    useEffect(() => {
    }, [members]);

    const handleAvatarChange = (e) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            
            if (!file.type.startsWith('image/')) {
                setError('Only image files are allowed');
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                setError('Profile picture must be less than 5MB');
                return;
            }
            
            setError(null);
            setSelectedAvatar(file);
            
            const reader = new FileReader();
            reader.onload = (e) => {
                setAvatarPreview(e.target.result);
            };
            reader.readAsDataURL(file);
        }
    };

    const handleSaveChanges = async () => {
        if (!groupName.trim()) {
            setError('Group name cannot be empty');
            setGroupName(group?.name || '');
            return;
        }
        
        setIsLoading(true);
        setError(null);
        
        try {
            // If name changed, update it using the proper endpoint
            if (groupName.trim() !== group.name) {
                const response = await apiClient.updateGroupName(group.id, {
                    name: groupName.trim()
                }, currentUser.id);
                
                if (!response.ok) {
                    throw new Error('Failed to update group name');
                }
            }
            
            // If avatar changed, update it using the profile-picture endpoint
            if (selectedAvatar) {
                const response = await apiClient.uploadGroupProfilePicture(group.id, selectedAvatar);
                
                if (!response.ok) {
                    throw new Error('Failed to update group picture');
                }
            }
            
            // Reset states after successful updates
            setSelectedAvatar(null);
            setAvatarPreview(null);
            
            onClose();
        } catch (error) {
            console.error('Error saving changes:', error);
            setError('Failed to save changes. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleAddMembers = (selectedUserIds) => {
        if (Array.isArray(selectedUserIds) && selectedUserIds.length > 0) {
            // Extract original IDs by removing the 'user-' prefix
            const processedUserIds = selectedUserIds
                .map(id => {
                    // Look up the original ID from our available users
                    const userChat = availableUsers.find(u => u.id === id);
                    return userChat ? userChat.originalId : null;
                })
                .filter(id => id !== null)
                .map(id => Number(id))
                .filter(id => !isNaN(id) && id > 0);

            if (processedUserIds.length > 0) {
                onAddMembers(processedUserIds);
            }
        }
        setShowAddMembersModal(false);
    };

    const handleRemoveMember = (memberId) => {
        const member = members.find(m => m.id === memberId);
        setMemberToRemove(member);
        setShowRemoveMemberConfirm(true);
    };

    const confirmRemoveMember = async () => {
        if (!memberToRemove) return;
        
        setIsLoading(true);
        setError(null);
        
        try {
            await onRemoveMember(memberToRemove.id);
            setShowRemoveMemberConfirm(false);
            setMemberToRemove(null);
        } catch (error) {
            console.error('Error removing member:', error);
            setError('Failed to remove member. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeleteGroup = async () => {
        setIsLoading(true);
        setError(null);
        
        try {
            await onDeleteGroup(group.id);
            onClose();
        } catch (error) {
            console.error('Error deleting group:', error);
            setError('Failed to delete group. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    // Fetch all users for adding as members
    const fetchAllUsers = async () => {
        setLoadingUsers(true);
        try {
            // Fetch both online and offline users
            const [onlineResponse, offlineResponse] = await Promise.all([
                apiClient.get('/users/list/ONLINE'),
                apiClient.get('/users/list/OFFLINE')
            ]);
            
            let users = [];
            
            if (onlineResponse.ok) {
                const onlineUsers = await onlineResponse.json();
                users = [...users, ...onlineUsers];
            }
            
            if (offlineResponse.ok) {
                const offlineUsers = await offlineResponse.json();
                users = [...users, ...offlineUsers];
            }
            
            // Remove duplicates based on user ID
            const uniqueUsers = users.filter((user, index, self) => 
                index === self.findIndex(u => u.id === user.id)
            );
            
            const validUsers = uniqueUsers.filter(user => user && user.id && user.userName);
            
            // Convert users to chat format for ContactSelectionModal
            const userChats = validUsers
                .filter(user => {
                    // Exclude current user and existing group members
                    return user.id !== currentUser.id && !members.some(member => member.id === user.id);
                })
                .map(user => ({
                    id: `user-${user.id}`, // Ensure unique key namespace for users
                    originalId: user.id, // Keep original ID for API calls
                    name: user.userName,
                    profilePicture: user.profilePictureUrl,
                    chatRoomType: 'PERSONAL',
                    participants: [user],
                    lastMessage: '',
                    timestamp: ''
                }));
            
            setAvailableUsers(userChats);
        } catch (error) {
            console.error('Error fetching users:', error);
            setError('Failed to load users. Please try again.');
        } finally {
            setLoadingUsers(false);
        }
    };

    // Handle opening add members modal
    const handleOpenAddMembersModal = () => {
        setShowAddMembersModal(true);
        fetchAllUsers();
    };

    if (!isOpen) return null;

    const isCreator = currentUser?.id === group?.createdById;

    return (
        <AnimatePresence>
            {isOpen && (
                <motion.div
                    className="group-settings-overlay"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={onClose}
                >
                    <motion.div
                        className="group-settings-modal"
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.9 }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="group-settings-header">
                            <h3 className="group-settings-title">Group Settings</h3>
                            <button className="group-settings-close-btn" onClick={onClose} disabled={isLoading}>
                                <img src="/cross-icon.png" alt="Close" />
                            </button>
                        </div>

                        {error && (
                            <div className="group-settings-error">
                                <p>{error}</p>
                            </div>
                        )}

                        <div className="group-settings-content">
                            <div className="group-profile-section">
                                <div className="group-avatar-container" onClick={() => isCreator && !isLoading && avatarInputRef.current.click()}>
                                    <Avatar
                                        src={avatarPreview || group?.profilePictureUrl}
                                        name={group.name}
                                        size={100}
                                    />
                                    {isCreator && !isLoading && (
                                        <div className="avatar-edit-overlay">
                                            <img src="/pen-solid.svg" alt="Edit" />
                                        </div>
                                    )}
                                </div>
                                <input
                                    type="file"
                                    ref={avatarInputRef}
                                    onChange={handleAvatarChange}
                                    style={{ display: 'none' }}
                                    accept="image/*"
                                    disabled={!isCreator || isLoading}
                                />
                                <div className="group-name-container">
                                    {isEditingName ? (
                                        <input
                                            type="text"
                                            value={groupName}
                                            onChange={(e) => setGroupName(e.target.value)}
                                            onBlur={() => setIsEditingName(false)}
                                            onKeyDown={(e) => {
                                                if (e.key === 'Enter') {
                                                    setIsEditingName(false);
                                                }
                                                if (e.key === 'Escape') {
                                                    setGroupName(group?.name || '');
                                                    setIsEditingName(false);
                                                }
                                            }}
                                            autoFocus
                                            className="group-name-input"
                                            disabled={!isCreator || isLoading}
                                        />
                                    ) : (
                                        <h2 className="group-name" onClick={() => isCreator && !isLoading && setIsEditingName(true)}>
                                            {groupName}
                                            {isCreator && !isLoading && <span className="edit-icon-wrapper"><img src="/pen-solid.svg" alt="Edit" className="edit-icon" /></span>}
                                        </h2>
                                    )}
                                    {isCreator && !isLoading && <p className="edit-prompt">Click to edit</p>}
                                </div>
                            </div>

                            <div className="members-section">
                                <div className="members-header">
                                    <h4>Members ({members.length})</h4>
                                    {isCreator && !isLoading && (
                                        <button className="add-members-btn" onClick={() => handleOpenAddMembersModal()}>
                                            <img src="/plus-solid.svg" alt="Add" />
                                            Add Members
                                        </button>
                                    )}
                                </div>
                                <div className="members-list">
                                    {members.filter(member => member && member.id).map((member, index) => {
                                        const memberKey = `member-${group?.id || 'unknown'}-${member.id}-${index}`;
                                        return (
                                            <div key={memberKey} className="member-item">
                                                <Avatar src={member.profilePictureUrl} name={member.userName} size={40} />
                                                <div className="member-info">
                                                    <span className="member-name">{member.userName}</span>
                                                    {member.id === group?.createdById && <span className="admin-badge">Admin</span>}
                                                </div>
                                                {isCreator && member.id !== currentUser.id && !isLoading && (
                                                    <button className="remove-member-btn" onClick={() => handleRemoveMember(member.id)}>
                                                        Remove
                                                    </button>
                                                )}
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>

                        <div className="group-settings-footer">
                            {isCreator && !isLoading && (
                                <button className="delete-group-btn" onClick={() => setShowDeleteConfirm(true)}>
                                    Delete Group
                                </button>
                            )}
                            <button 
                                className="save-changes-btn" 
                                onClick={handleSaveChanges}
                                disabled={isLoading}
                            >
                                {isLoading ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>

                        {showDeleteConfirm && (
                            <div className="delete-confirm-dialog">
                                <p>Are you sure you want to delete this group permanently?</p>
                                <div className="delete-confirm-actions">
                                    <button onClick={() => setShowDeleteConfirm(false)} disabled={isLoading}>
                                        Cancel
                                    </button>
                                    <button 
                                        onClick={handleDeleteGroup} 
                                        className="confirm-delete-btn"
                                        disabled={isLoading}
                                    >
                                        {isLoading ? 'Deleting...' : 'Delete'}
                                    </button>
                                </div>
                            </div>
                        )}
                    </motion.div>
                </motion.div>
            )}
            {showAddMembersModal && (
                <ContactSelectionModal
                    isOpen={showAddMembersModal}
                    onClose={() => setShowAddMembersModal(false)}
                    onAction={handleAddMembers}
                    currentUser={currentUser}
                    mode="addMembers"
                    title="Add New Members"
                    subtitle={loadingUsers ? "Loading users..." : "Choose contacts to add to the group"}
                    actionButtonText="Add Members"
                    chats={availableUsers}
                />
            )}
            
            {/* Remove Member Confirmation Modal */}
            <ConfirmationModal
                isOpen={showRemoveMemberConfirm}
                onClose={() => setShowRemoveMemberConfirm(false)}
                onConfirm={confirmRemoveMember}
                title="Remove Member"
                message={`Are you sure you want to remove ${memberToRemove?.userName || 'this member'} from the group?`}
                type="warning"
                isLoading={isLoading}
                confirmText="Remove"
                cancelText="Cancel"
            />
        </AnimatePresence>
    );
};

export default GroupSettingsModal;
