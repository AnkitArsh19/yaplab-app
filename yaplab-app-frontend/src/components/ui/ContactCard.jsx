import React from 'react';
import '../../styles/ContactCard.css';
import Avatar from './Avatar';

function ContactCard({ profilePicture, name, lastMessage, timestamp, isSelected, onClick, animationDelay = 0 }) {
    return (
        <div 
            className={`contactCard ${isSelected ? 'selected' : ''}`}
            onClick={onClick}
            style={{
                animationDelay: `${animationDelay}s`
            }}
        >
            <Avatar
                src={profilePicture}
                name={name}
                size={45}
                className="avatar"
            />
            
            <div className="info">
                <p className="name">{name}</p>
                <p className="last-message">{lastMessage}</p>
            </div>
            <p className="timestamp">{timestamp}</p>
        </div>
    );
}

export default ContactCard;
