import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import Avatar from '../ui/Avatar';
import UserInfoModal from '../modals/UserInfoModal';
import '../../styles/GroupInfoModal.css';

const GroupInfoModal = ({ 
    isOpen, 
    onClose, 
    group, 
    members = [],
    currentUser,
    onMemberClick 
}) => {
    const [showMemberInfo, setShowMemberInfo] = useState(false);
    const [selectedMember, setSelectedMember] = useState(null);

    if (!isOpen || !group) return null;

    // Format group creation date
    const formatCreationDate = (date) => {
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

    // Get group creator name
    const getCreatorName = () => {
        if (!group.createdById) return 'Unknown';
        const creator = members.find(member => member.id === group.createdById);
        return creator ? creator.userName || creator.name : 'Unknown';
    };

    // Get user initials for default avatar
    const getUserInitials = (name) => {
        if (!name) return '?';
        const names = name.split(' ');
        if (names.length >= 2) {
            return (names[0][0] + names[names.length - 1][0]).toUpperCase();
        }
        return name[0].toUpperCase();
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

    const groupInitials = getUserInitials(group.name);

    return (
        <>
            <AnimatePresence>
                <motion.div 
                    className="group-info-overlay"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    onClick={onClose}
                >
                    <motion.div 
                        className="group-info-modal"
                        initial={{ opacity: 0, scale: 0.9, y: -20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.9, y: -20 }}
                        onClick={(e) => e.stopPropagation()}
                    >
                        <div className="group-info-header">
                            <h3 className="group-info-title">Group Info</h3>
                            <button className="group-info-close-btn" onClick={onClose}>
                                <img src="/cross-icon.png" alt="Close" />
                            </button>
                        </div>

                        <div className="group-info-profile-section">
                            <div className="group-info-picture-container">
                                <Avatar
                                    src={group.profilePictureUrl}
                                    name={group.name}
                                    size={120}
                                    isGroup={true}
                                />
                            </div>
                        </div>

                        <div className="group-info-content">
                            <div className="group-info-field">
                                <label className="group-info-label">Group Name</label>
                                <div className="group-info-value">
                                    {group.name || 'Unnamed Group'}
                                </div>
                            </div>

                            <div className="group-info-field">
                                <label className="group-info-label">Created By</label>
                                <div className="group-info-value">
                                    {getCreatorName()}
                                </div>
                            </div>

                            <div className="group-info-field">
                                <label className="group-info-label">Created On</label>
                                <div className="group-info-value">
                                    {formatCreationDate(group.createdAt)}
                                </div>
                            </div>

                            <div className="group-info-field">
                                <label className="group-info-label">Members</label>
                                <div className="group-info-value">
                                    {members.length} member{members.length !== 1 ? 's' : ''}
                                </div>
                            </div>
                        </div>

                        <div className="group-members-section">
                            <h4 className="group-members-title">Members</h4>
                            <div className="group-members-list">
                                {members.map((member, index) => {
                                    const isCreator = member.id === group.createdById;
                                    
                                    return (
                                        <div 
                                            key={member.id || index} 
                                            className="group-member-item"
                                            onClick={() => handleMemberClick(member)}
                                        >
                                            <div className="group-member-avatar">
                                                <Avatar
                                                    src={member.profilePictureUrl}
                                                    name={member.userName || member.name}
                                                    size={40}
                                                />
                                            </div>
                                            <div className="group-member-info">
                                                <div className="group-member-name">
                                                    {member.userName || member.name || 'Unknown'}
                                                    {isCreator && <span className="creator-badge">Admin</span>}
                                                </div>
                                                {member.emailId && (
                                                    <div className="group-member-email">
                                                        {member.emailId}
                                                    </div>
                                                )}
                                            </div>
                                            <div className="group-member-arrow">
                                                <img src="/chevron-down-solid.svg" alt="View" />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                        <div className="group-info-footer">
                            <button 
                                className="group-info-close-footer-btn" 
                                onClick={onClose}
                            >
                                Close
                            </button>
                        </div>
                    </motion.div>
                </motion.div>
            </AnimatePresence>

            {showMemberInfo && selectedMember && (
                <UserInfoModal
                    isOpen={showMemberInfo}
                    onClose={handleCloseMemberInfo}
                    user={selectedMember}
                    mode="view"
                    title={`${selectedMember.userName || selectedMember.name}'s Info`}
                />
            )}
        </>
    );
};

export default GroupInfoModal;
