import React from 'react';
import '../Style/ContactList.css';

function ContactList({contacts}){

    if(!contacts || contacts.length === 0) {
        return <p className='noContacts'>Search for your friend to chat.</p>;
    }

    return (
        <>
            <div className='contactList'>
                {contacts.map((contact, index) => (
                    <div key={index} className='userInfoList'>
                        <img src={contact.avatar} alt={`${contact.username}'s avatar`} className='useravatar' />
                        <span className='contactName'>{contact.username}</span>
                    </div>    
                ))}
            </div>
        
        </>
    );
}

export default ContactList