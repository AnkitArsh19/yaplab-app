import React from 'react';
import '../Style/ContactCard.css';

function ContactCard({profilePicture, name, lastMessage, timestamp, onClick}){

    return (
        <>
            <div className='contactCard' onClick={onClick}>
                <img src ={profilePicture} alt={name} className="avatar"/>
                <div className="info">
                    <p className="name">{name}</p>
                    <p className="last-message">{lastMessage}</p>
                </div>
                <p className="timestamp">{timestamp}</p>
            </div>
        
        </>
    );
}

export default ContactCard