import React from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import '../../styles/ConfirmationModal.css';

function ConfirmationModal({ 
    isOpen, 
    onClose, 
    onConfirm, 
    title = 'Confirm', 
    message, 
    confirmText = 'Confirm', 
    cancelText = 'Cancel',
    type = 'default',
    isLoading = false
}) {
    const buttonClasses = {
        danger: 'confirm-modal-button danger',
        warning: 'confirm-modal-button warning',
        default: 'confirm-modal-button primary'
    };

    return createPortal(
        <AnimatePresence>
            {isOpen && (
                <motion.div 
                    className="confirm-modal-backdrop"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.2 }}
                    onClick={e => e.target === e.currentTarget && onClose()}
                >
                    <motion.div 
                        className="confirm-modal-container"
                        initial={{ scale: 0.8, opacity: 0, y: 20 }}
                        animate={{ scale: 1, opacity: 1, y: 0 }}
                        exit={{ scale: 0.8, opacity: 0, y: 20 }}
                        transition={{ 
                            type: "spring", 
                            stiffness: 300, 
                            damping: 25,
                            duration: 0.3 
                        }}
                    >
                        <div className="confirm-modal-content">
                            <h3 className="confirm-modal-title">{title}</h3>
                            <p className="confirm-modal-message">{message}</p>
                            <div className="confirm-modal-buttons">
                                <button 
                                    className="confirm-modal-button secondary"
                                    onClick={onClose}
                                    disabled={isLoading}
                                >
                                    {cancelText}
                                </button>
                                <button 
                                    className={buttonClasses[type] || buttonClasses.default}
                                    onClick={onConfirm}
                                    disabled={isLoading}
                                >
                                    {isLoading ? 'Deleting...' : confirmText}
                                </button>
                            </div>
                        </div>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>,
        document.body
    );
}

export default ConfirmationModal;
