import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../utils/apiClient';
import Notification from '../ui/Notification';
import LoadingThreeDots from '../ui/LoadingThreeDots';
import '../../styles/UserSettingsModal.css';

const UserSettingsModal = ({ 
    isOpen, 
    onClose, 
    currentUser, 
    onSave,
    showNotification = null 
}) => {
    const [formData, setFormData] = useState({
        email: '',
        phoneNumber: '',
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
    });
    
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [showEmailChangeConfirm, setShowEmailChangeConfirm] = useState(false);
    const [emailChangePassword, setEmailChangePassword] = useState('');
    const [activeSection, setActiveSection] = useState('account');
    const [isLoading, setIsLoading] = useState(false);
    const [completeUserData, setCompleteUserData] = useState(null);
    const [notification, setNotification] = useState({ message: '', type: '', show: false });
    const [originalData, setOriginalData] = useState({ email: '', phoneNumber: '' });
    const [showOldPassword, setShowOldPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [showEmailChangePassword, setShowEmailChangePassword] = useState(false);

    // Helper function to show notifications
    const showNotificationMessage = (message, type = 'info') => {
        if (showNotification) {
            showNotification(message, type);
        } else {
            setNotification({ message, type, show: true });
        }
    };

    // Helper function to close internal notification
    const handleCloseNotification = () => {
        setNotification({ message: '', type: '', show: false });
    };

    // Helper function to check if form data has changed
    const hasFormDataChanged = () => {
        return formData.email !== originalData.email || 
               formData.phoneNumber !== originalData.phoneNumber;
    };

    // Fetch complete user data when modal opens
    useEffect(() => {
        const fetchCompleteUserData = async () => {
            if (isOpen && currentUser?.id && !completeUserData) {
                try {
                    setIsLoading(true);
                    const response = await apiClient.get(`/users/${currentUser.id}`);
                    if (response.ok) {
                        const userData = await response.json();
                        setCompleteUserData(userData);
                        const formValues = {
                            email: userData.emailId || '',
                            phoneNumber: userData.mobileNumber || '',
                            oldPassword: '',
                            newPassword: '',
                            confirmPassword: ''
                        };
                        setFormData(formValues);
                        setOriginalData({
                            email: userData.emailId || '',
                            phoneNumber: userData.mobileNumber || ''
                        });
                    }
        } catch (error) {
            console.error('Error fetching complete user data:', error);
            const fallbackValues = {
                email: currentUser.emailId || currentUser.email || '',
                phoneNumber: currentUser.mobileNumber || currentUser.phoneNumber || '',
                oldPassword: '',
                newPassword: '',
                confirmPassword: ''
            };
            setFormData(fallbackValues);
            setOriginalData({
                email: currentUser.emailId || currentUser.email || '',
                phoneNumber: currentUser.mobileNumber || currentUser.phoneNumber || ''
            });
        } finally {
            setIsLoading(false);
        }
            }
        };

        fetchCompleteUserData();
    }, [isOpen, currentUser?.id, completeUserData]);

    // Reset form data when modal closes
    useEffect(() => {
        if (!isOpen) {
            setCompleteUserData(null);
            setFormData({
                email: '',
                phoneNumber: '',
                oldPassword: '',
                newPassword: '',
                confirmPassword: ''
            });
        }
    }, [isOpen]);

    const handleClose = () => {
        setFormData({
            email: '',
            phoneNumber: '',
            oldPassword: '',
            newPassword: '',
            confirmPassword: ''
        });
        setOriginalData({ email: '', phoneNumber: '' });
        setCompleteUserData(null);
        setShowDeleteConfirm(false);
        setShowEmailChangeConfirm(false);
        setEmailChangePassword('');
        setActiveSection('account');
        setNotification({ message: '', type: '', show: false });
        setShowOldPassword(false);
        setShowNewPassword(false);
        setShowConfirmPassword(false);
        setShowEmailChangePassword(false);
        onClose();
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSaveAccountInfo = async (e) => {
        e.preventDefault();
        
        if (!hasFormDataChanged()) {
            showNotificationMessage('No changes detected.... Please make changes before saving.', 'info');
            return;
        }
        
        setIsLoading(true);
        
        try {
            if (!formData.email.trim()) {
                throw new Error('Email is required');
            }
            
            if (formData.phoneNumber.trim()) {
                const phoneRegex = /^[0-9]{10}$/;
                if (!phoneRegex.test(formData.phoneNumber.trim())) {
                    throw new Error('Phone number must be exactly 10 digits');
                }
            }
            
            const currentEmail = originalData.email;
            const newEmail = formData.email.trim();
            const newPhoneNumber = formData.phoneNumber.trim();
            
            if (newEmail !== currentEmail) {
                setShowEmailChangeConfirm(true);
                return;
            }
            
            // Handle phone number change (only if email is not changing)
            const userUpdateData = {
                id: completeUserData?.id || currentUser.id,
                userName: completeUserData?.userName || currentUser.userName,
                emailId: currentEmail,
                mobileNumber: newPhoneNumber || "",
                password: "dummyPassword123"
            };
            console.log('Sending phone number update:', userUpdateData);

            const response = await apiClient.put('/users/update', userUpdateData);
            
            if (response.ok) {
                const updatedUser = await response.json();
                
                setOriginalData({
                    email: updatedUser.emailId,
                    phoneNumber: updatedUser.mobileNumber || ''
                });
                
                setFormData(prev => ({
                    ...prev,
                    email: updatedUser.emailId,
                    phoneNumber: updatedUser.mobileNumber || ''
                }));
                
                showNotificationMessage('Phone number updated successfully!', 'success');
                
                if (onSave) {
                    await onSave({
                        action: 'updateAccount',
                        data: updatedUser
                    });
                }
            } else {
                const errorData = await response.json();
                console.error('Update failed:', errorData);
                
                let errorMessage = errorData.message || 'Failed to update phone number';
                
                if (errorMessage.includes('already in use')) {
                    showNotificationMessage(errorMessage, 'error');
                } else if (errorMessage.includes('duplicate') || errorMessage.includes('Duplicate')) {
                    if (errorMessage.includes('mobile') || errorMessage.includes('phone')) {
                        showNotificationMessage('This phone number is already registered with another account.', 'error');
                    } else {
                        showNotificationMessage('This information is already registered with another account.', 'error');
                    }
                } else {
                    showNotificationMessage(errorMessage, 'error');
                }
                
                throw new Error(errorMessage);
            }
        } catch (error) {
            console.error('Error updating account:', error);
            showNotificationMessage(`Failed to update account information: ${error.message}`, 'error');
        } finally {
            setIsLoading(false);
        }
    };

    const handleEmailChangeConfirm = async () => {
        if (!emailChangePassword.trim()) {
            showNotificationMessage('Please enter your current password to verify the email change.', 'error');
            return;
        }
        
        setIsLoading(true);
        
        try {
            const emailChangeRequest = {
                newEmail: formData.email.trim(),
                currentPassword: emailChangePassword
            };
            
            const emailResponse = await apiClient.post('/users/initiate-email-change', emailChangeRequest);
            
            if (emailResponse.ok) {
                const result = await emailResponse.json();
                showNotificationMessage(result.message, 'success');
                
                setShowEmailChangeConfirm(false);
                setEmailChangePassword('');
                
                setFormData(prev => ({
                    ...prev,
                    email: originalData.email
                }));
            } else {
                const errorData = await emailResponse.json();
                throw new Error(errorData.error || 'Failed to initiate email change');
            }
        } catch (error) {
            console.error('Error initiating email change:', error);
            showNotificationMessage(`Failed to initiate email change: ${error.message}`, 'error');
        } finally {
            setIsLoading(false);
        }
    };

    const handleEmailChangeCancel = () => {
        setShowEmailChangeConfirm(false);
        setEmailChangePassword('');
        setShowEmailChangePassword(false);
        setFormData(prev => ({
            ...prev,
            email: originalData.email
        }));
        setIsLoading(false);
    };

    const handleChangePassword = async (e) => {
        e.preventDefault();
        
        if (!formData.oldPassword || !formData.newPassword || !formData.confirmPassword) {
            showNotificationMessage('Please fill in all password fields', 'error');
            return;
        }
        
        if (formData.newPassword !== formData.confirmPassword) {
            showNotificationMessage('New passwords do not match', 'error');
            return;
        }
        
        if (formData.newPassword.length < 6) {
            showNotificationMessage('Password must be at least 6 characters long', 'error');
            return;
        }
        
        setIsLoading(true);
        
        try {
            const passwordChangeData = {
                emailId: currentUser.emailId || currentUser.email,
                oldPassword: formData.oldPassword,
                newPassword: formData.newPassword
            };

            const response = await apiClient.post('/auth/change-password', passwordChangeData);
            
            if (response.ok) {
                setFormData(prev => ({
                    ...prev,
                    oldPassword: '',
                    newPassword: '',
                    confirmPassword: ''
                }));
                
                setShowOldPassword(false);
                setShowNewPassword(false);
                setShowConfirmPassword(false);
                
                showNotificationMessage('Password changed successfully!', 'success');
                
                if (onSave) {
                    await onSave({
                        action: 'changePassword',
                        data: { success: true }
                    });
                }
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to change password');
            }
        } catch (error) {
            console.error('Error changing password:', error);
            showNotificationMessage(`Failed to change password: ${error.message}`, 'error');
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeleteAccount = async () => {
        setIsLoading(true);
        
        try {
            const response = await apiClient.delete(`/users/${currentUser.id}`);
            
            if (response.ok) {
                if (onSave) {
                    await onSave({
                        action: 'deleteAccount',
                        data: { success: true }
                    });
                }
                
                localStorage.removeItem('authToken');
                localStorage.removeItem('refreshToken');
                localStorage.removeItem('currentUser');
                
                showNotificationMessage('Account deleted successfully. You will be redirected to the login page.', 'success');
                
                handleClose();
                
                setTimeout(() => {
                    window.location.href = '/login';
                }, 1000);
            } else {
                const errorData = await response.json();
                throw new Error(errorData.message || 'Failed to delete account');
            }
        } catch (error) {
            console.error('Error deleting account:', error);
            showNotificationMessage(`Failed to delete account: ${error.message}`, 'error');
        } finally {
            setIsLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <AnimatePresence>
            <motion.div 
                className="user-settings-overlay"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={handleClose}
            >
                <motion.div 
                    className="user-settings-modal"
                    initial={{ opacity: 0, scale: 0.9, y: -20 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.9, y: -20 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="user-settings-header">
                        <h2 className="user-settings-title">Account Settings</h2>
                        <button className="user-settings-close-btn" onClick={handleClose}>
                            <img src="/cross-icon.png" alt="Close" />
                        </button>
                    </div>

                    {/* Navigation Tabs */}
                    <div className="user-settings-tabs">
                        <button 
                            className={`user-settings-tab ${activeSection === 'account' ? 'active' : ''}`}
                            onClick={() => setActiveSection('account')}
                        >
                            <img src="/user-pen-solid.svg" alt="" />
                            Account Info
                        </button>
                        <button 
                            className={`user-settings-tab ${activeSection === 'password' ? 'active' : ''}`}
                            onClick={() => setActiveSection('password')}
                        >
                            <img src="/gear-solid.svg" alt="" />
                            Security
                        </button>
                        <button 
                            className={`user-settings-tab ${activeSection === 'danger' ? 'active' : ''}`}
                            onClick={() => setActiveSection('danger')}
                        >
                            <img src="/delete-icon.svg" alt="" />
                            Danger Zone
                        </button>
                    </div>

                    {/* Content Sections */}
                    <div className="user-settings-content">
                        {activeSection === 'account' && (
                            <motion.div
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -20 }}
                                className="settings-section"
                            >
                                <form onSubmit={handleSaveAccountInfo} className="settings-form">
                                    <h3 className="section-title">Contact Information</h3>
                                    <p className="section-description">
                                        Update your email address and phone number. Email is required, phone number is optional.
                                    </p>
                                    
                                    <div className="form-group">
                                        <label htmlFor="email">Email Address *</label>
                                        <input
                                            type="email"
                                            id="email"
                                            name="email"
                                            value={formData.email}
                                            onChange={handleInputChange}
                                            className="settings-input"
                                            required
                                            disabled={isLoading}
                                        />
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="phoneNumber">Phone Number</label>
                                        <input
                                            type="tel"
                                            id="phoneNumber"
                                            name="phoneNumber"
                                            value={formData.phoneNumber}
                                            onChange={handleInputChange}
                                            className="settings-input"
                                            placeholder="Enter your 10-digit phone number"
                                            disabled={isLoading}
                                        />
                                    </div>

                                    <div className="form-actions">
                                        <button 
                                            type="submit" 
                                            className="save-btn"
                                            disabled={isLoading}
                                        >
                                            {isLoading ? (
                                                <LoadingThreeDots />
                                            ) : (
                                                'Save Changes'
                                            )}
                                        </button>
                                    </div>
                                </form>
                            </motion.div>
                        )}

                        {activeSection === 'password' && (
                            <motion.div
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -20 }}
                                className="settings-section"
                            >
                                <form onSubmit={handleChangePassword} className="settings-form">
                                    <h3 className="section-title">Change Password</h3>
                                    <p className="section-description">
                                        Ensure your account is using a long, random password to stay secure.
                                    </p>
                                    
                                    <div className="form-group">
                                        <label htmlFor="oldPassword">Current Password</label>
                                        <div className="passwordfield">
                                            <input
                                                type={showOldPassword ? "text" : "password"}
                                                id="oldPassword"
                                                name="oldPassword"
                                                value={formData.oldPassword}
                                                onChange={handleInputChange}
                                                className="settings-input-password"
                                                required
                                                disabled={isLoading}
                                                placeholder="Enter your current password"
                                                autoComplete="current-password"
                                            />
                                            <div className="eye-container">
                                                <img
                                                    className="eye"
                                                    src={showOldPassword ? "/eye.svg" : "/eye-closed.svg"}
                                                    alt={showOldPassword ? "Hide password" : "Show password"}
                                                    onClick={() => setShowOldPassword(prev => !prev)}
                                                />
                                            </div>
                                        </div>
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="newPassword">New Password</label>
                                        <div className="passwordfield">
                                            <input
                                                type={showNewPassword ? "text" : "password"}
                                                id="newPassword"
                                                name="newPassword"
                                                value={formData.newPassword}
                                                onChange={handleInputChange}
                                                className="settings-input-password"
                                                required
                                                disabled={isLoading}
                                                placeholder="Enter new password (min 6 characters)"
                                                autoComplete="new-password"
                                            />
                                            <div className="eye-container">
                                                <img
                                                    className="eye"
                                                    src={showNewPassword ? "/eye.svg" : "/eye-closed.svg"}
                                                    alt={showNewPassword ? "Hide password" : "Show password"}
                                                    onClick={() => setShowNewPassword(prev => !prev)}
                                                />
                                            </div>
                                        </div>
                                    </div>

                                    <div className="form-group">
                                        <label htmlFor="confirmPassword">Confirm New Password</label>
                                        <div className="passwordfield">
                                            <input
                                                type={showConfirmPassword ? "text" : "password"}
                                                id="confirmPassword"
                                                name="confirmPassword"
                                                value={formData.confirmPassword}
                                                onChange={handleInputChange}
                                                className="settings-input-password"
                                                required
                                                disabled={isLoading}
                                                placeholder="Confirm your new password"
                                                autoComplete="new-password"
                                            />
                                            <div className="eye-container">
                                                <img
                                                    className="eye"
                                                    src={showConfirmPassword ? "/eye.svg" : "/eye-closed.svg"}
                                                    alt={showConfirmPassword ? "Hide password" : "Show password"}
                                                    onClick={() => setShowConfirmPassword(prev => !prev)}
                                                />
                                            </div>
                                        </div>
                                    </div>

                                    <div className="form-actions">
                                        <button 
                                            type="submit" 
                                            className="save-btn"
                                            disabled={isLoading}
                                        >
                                            {isLoading ? (
                                                <LoadingThreeDots />
                                            ) : (
                                                'Change Password'
                                            )}
                                        </button>
                                    </div>
                                </form>
                            </motion.div>
                        )}

                        {activeSection === 'danger' && (
                            <motion.div
                                initial={{ opacity: 0, x: 20 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -20 }}
                                className="settings-section"
                            >
                                <div className="danger-zone">
                                    <h3 className="section-title">Delete Account</h3>
                                    <p className="danger-warning">
                                        Once you delete your account, there is no going back. Please be certain.
                                        All your messages, groups, and personal data will be permanently removed.
                                    </p>
                                    
                                    {!showDeleteConfirm ? (
                                        <button 
                                            className="delete-btn"
                                            onClick={() => setShowDeleteConfirm(true)}
                                            disabled={isLoading}
                                        >
                                            Delete My Account
                                        </button>
                                    ) : (
                                        <div className="delete-confirmation">
                                            <p className="confirm-text">
                                                Are you absolutely sure you want to delete your account?
                                                This action cannot be undone.
                                            </p>
                                            <div className="confirm-actions">
                                                <button 
                                                    className="confirm-delete-btn"
                                                    onClick={handleDeleteAccount}
                                                    disabled={isLoading}
                                                >
                                                    {isLoading ? (
                                                        <LoadingThreeDots />
                                                    ) : (
                                                        'Yes, Delete Forever'
                                                    )}
                                                </button>
                                                <button 
                                                    className="cancel-btn"
                                                    onClick={() => setShowDeleteConfirm(false)}
                                                    disabled={isLoading}
                                                >
                                                    Cancel
                                                </button>
                                            </div>
                                        </div>
                                    )}
                                </div>
                            </motion.div>
                        )}
                    </div>

                    {/* Email Change Confirmation Modal */}
                    {showEmailChangeConfirm && (
                        <motion.div 
                            className="email-change-modal"
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            exit={{ opacity: 0, scale: 0.9 }}
                        >
                            <div className="email-change-content">
                                <h3>Confirm Email Change</h3>
                                <p>To change your email from <strong>{originalData.email}</strong> to <strong>{formData.email}</strong>, please enter your current password to verify this change.</p>
                                
                                <div className="form-group">
                                    <label htmlFor="emailChangePassword">Current Password</label>
                                    <div className="passwordfield">
                                        <input
                                            type={showEmailChangePassword ? "text" : "password"}
                                            id="emailChangePassword"
                                            value={emailChangePassword}
                                            onChange={(e) => setEmailChangePassword(e.target.value)}
                                            className="settings-input-password"
                                            placeholder="Enter your current password"
                                            disabled={isLoading}
                                            autoComplete="off"
                                            data-lpignore="true"
                                        />
                                        <div className="eye-container">
                                            <img
                                                className="eye"
                                                src={showEmailChangePassword ? "/eye.svg" : "/eye-closed.svg"}
                                                alt={showEmailChangePassword ? "Hide password" : "Show password"}
                                                onClick={() => setShowEmailChangePassword(prev => !prev)}
                                            />
                                        </div>
                                    </div>
                                </div>
                                
                                <div className="email-change-actions">
                                    <button 
                                        className="save-btn"
                                        onClick={handleEmailChangeConfirm}
                                        disabled={isLoading}
                                    >
                                        {isLoading ? (
                                            <LoadingThreeDots />
                                        ) : (
                                            'Confirm Email Change'
                                        )}
                                    </button>
                                    <button 
                                        className="cancel-btn"
                                        onClick={handleEmailChangeCancel}
                                        disabled={isLoading}
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </div>
                        </motion.div>
                    )}

                    {/* Internal Notification */}
                    {notification.show && (
                        <Notification 
                            message={notification.message} 
                            type={notification.type} 
                            onClose={handleCloseNotification}
                        />
                    )}
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
};

export default UserSettingsModal;
