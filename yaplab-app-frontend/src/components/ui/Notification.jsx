import React, { useEffect, useState } from 'react';
import '../../styles/Notification.css';

function Notification({ message, type = 'info', duration = 3000, onClose }) {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        if (message) {
            setIsVisible(true);
            const timer = setTimeout(() => {
                setIsVisible(false);
                setTimeout(() => {
                    onClose && onClose();
                }, 300);
            }, duration);

            return () => clearTimeout(timer);
        }
    }, [message, duration, onClose]);

    if (!message) return null;

    return (
        <div className={`notification ${type} ${isVisible ? 'show' : ''}`}>
            <div className="notification-content">
                <span className="notification-message">{message}</span>
            </div>
        </div>
    );
}

export default Notification;
