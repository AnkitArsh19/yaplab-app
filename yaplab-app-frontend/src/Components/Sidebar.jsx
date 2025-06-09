import React, { useState } from 'react';
import '../Style/Sidebar.css';
import ContactCard from './ContactCard';
import SettingsMenu from './SettingsMenu';

function Sidebar({ chats = [], onSelectChat, loading, error, onLogout }) {
    const [searchQuery, setSearchQuery] = useState('');

    const handleSearchChange = (e) => {
        setSearchQuery(e.target.value);
    };

    return (
        <div className='sidebar'>
            <div className="logo">
                <img className="logo-image" src="logo.png" alt="logo-icon" />
                <img className="logo-name" src="logoname.png" alt="logo-name" />
            </div>
            
            <div className='searchbar-container'>
                <SettingsMenu onLogout={onLogout} />
                <div className='searchbar'>
                    <input 
                        type="text" 
                        className='search' 
                        placeholder=' Search' 
                        value={searchQuery}
                        onChange={handleSearchChange}
                    />
                </div>
            </div>
            
            <br />
            <div className='chatHistory'>
                {loading ? (
                    <div className="loading-message">Loading your chats...</div>
                ) : error ? (
                    <div className="error-message">{error}</div>
                ) : chats.length === 0 ? (
                    <div className="empty-chats-message">
                        <p>No chat history found.</p>
                        <p>Search for contacts above to start a new chat!</p>
                    </div>
                ) : (
                    chats.map((chat) => (
                        <ContactCard
                            key={chat.id}
                            profilePicture={chat.profilePicture}
                            name={chat.name}
                            lastMessage={chat.lastMessage}
                            timestamp={chat.timestamp}
                            onClick={() => onSelectChat(chat.id)}
                        />
                    ))
                )}
            </div>
        </div>
    );
}

export default Sidebar;