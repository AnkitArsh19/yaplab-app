import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import apiClient from '../../utils/apiClient.js';
import Avatar from '../ui/Avatar.jsx';
import '../../styles/Searchbar.css';

function SearchBar({ onSelectContact, existingChats = [] }) {
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [showResults, setShowResults] = useState(false);
    const [isFocused, setIsFocused] = useState(false);
    const searchRef = useRef(null);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (searchRef.current && !searchRef.current.contains(event.target)) {
                setShowResults(false);
                setIsFocused(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    useEffect(() => {
        const delayedSearch = setTimeout(() => {
            if (searchQuery.trim().length > 0) {
                searchContacts(searchQuery.trim());
            } else {
                setSearchResults([]);
                if (isFocused) {
                    setShowResults(true);
                } else {
                    setShowResults(false);
                }
            }
        }, 300);

        return () => clearTimeout(delayedSearch);
    }, [searchQuery, isFocused]);  

    const searchContacts = async (query) => {
        setIsLoading(true);
        try {
            const response = await apiClient.get(`/users/search/${encodeURIComponent(query)}`);
            
            if (response.ok) {
                const data = await response.json();
                const validUsers = Array.isArray(data) ? data.filter(user => user && user.id && user.userName) : [];
                setSearchResults(validUsers);
                setShowResults(true);
            } else {
                console.error('Search failed with status:', response.status);
                setSearchResults([]);
                setShowResults(true);
            }
        } catch (error) {
            console.error('Error searching contacts:', error);
            setSearchResults([]);
            setShowResults(true);
        } finally {
            setIsLoading(false);
        }
    };

    const handleSearchChange = (e) => {
        const value = e.target.value;
        setSearchQuery(value);
    };

    const handleContactSelect = (contact) => {
        const existingChat = existingChats.find(chat => {
            return chat.participants && chat.participants.some(participant => participant.id === contact.id);
        });

        if (existingChat) {
            onSelectContact(contact, existingChat.id);
        } else {
            onSelectContact(contact);
        }
        
        setSearchQuery('');
        setSearchResults([]);
        setShowResults(false);
        setIsFocused(false);
    };

    const isContactInExistingChats = (contactId) => {
        return existingChats.some(chat => {
            return chat.participants && chat.participants.some(participant => participant.id === contactId);
        });
    };

    const handleFocus = () => {
        setIsFocused(true);
        setShowResults(true);
    };

    const handleBlur = () => {
        // Empty to prevent closing search results on blur
    };

    const resultsVariants = {
        hidden: {
            opacity: 0,
            y: -10,
            scale: 0.95
        },
        visible: {
            opacity: 1,
            y: 0,
            scale: 1,
            transition: {
                duration: 0.2,
                staggerChildren: 0.05
            }
        },
        exit: {
            opacity: 0,
            y: -10,
            scale: 0.95,
            transition: {
                duration: 0.15
            }
        }
    };

    const itemVariants = {
        hidden: {
            opacity: 0,
            x: -20
        },
        visible: {
            opacity: 1,
            x: 0,
            transition: {
                duration: 0.2
            }
        }
    };

    return (
        <div className="searchbar" ref={searchRef}>
            <div className="search-input-container">
                <div className='search-icon-div'>
                    <img src="/search-icon.png" alt="search" className="search-icon" />
                </div>
                
                <input 
                    type="text" 
                    className="search" 
                    value={searchQuery}
                    onChange={handleSearchChange}
                    onFocus={handleFocus}
                    onBlur={handleBlur}
                    placeholder="Search"
                />
                
                {searchQuery && (
                    <div 
                        className="search-clear-icon-div" 
                        onClick={() => {
                            setSearchQuery('');
                            setSearchResults([]);
                            setShowResults(false);
                        }}
                    >
                        <img src="/cross-icon.png" alt="clear" className="search-clear-icon" />
                    </div>
                )}
            </div>

            <AnimatePresence>
                {showResults && (
                    <motion.div 
                        className="search-results"
                        variants={resultsVariants}
                        initial="hidden"
                        animate="visible"
                        exit="exit"
                    >
                        {isLoading ? (
                            <motion.div 
                                className="search-no-results"
                                variants={itemVariants}
                            >
                                Searching...
                            </motion.div>
                        ) : searchResults.length > 0 ? (
                            searchResults.map((contact) => {
                                const hasExistingChat = isContactInExistingChats(contact.id);
                                return (
                                    <motion.div 
                                        key={contact.id} 
                                        className={`search-result-item ${hasExistingChat ? 'existing-chat' : ''}`}
                                        variants={itemVariants}
                                        whileHover={{ 
                                            scale: 1.02,
                                            x: 4,
                                            transition: { duration: 0.2 }
                                        }}
                                        whileTap={{ 
                                            scale: 0.98,
                                            transition: { duration: 0.1 }
                                        }}
                                        onClick={() => handleContactSelect(contact)}
                                    >
                                        <motion.div 
                                            className="search-result-avatar"
                                            whileHover={{ scale: 1.1 }}
                                        >
                                            <Avatar 
                                                src={contact.profilePictureUrl}
                                                name={contact.userName}
                                                size={40}
                                            />
                                        </motion.div>
                                        <motion.div 
                                            className="search-result-info"
                                            whileHover={{ x: 4 }}
                                        >
                                            <div className="search-result-name">
                                                {contact.userName || 'Unknown'}
                                                {hasExistingChat && <span className="existing-chat-indicator">• Active Chat</span>}
                                            </div>
                                            <div className="search-result-contact">
                                                • {contact.emailId} <br />
                                                • {contact.mobileNumber}
                                            </div>
                                        </motion.div>
                                    </motion.div>
                                );
                            })
                        ) : searchQuery.trim().length > 0 ? (
                            <motion.div 
                                className="search-no-results"
                                variants={itemVariants}
                            >
                                No contacts found
                            </motion.div>
                        ) : isFocused ? (
                            <motion.div 
                                className="search-no-results"
                                variants={itemVariants}
                            >
                                Enter name, email id or phone number
                            </motion.div>
                        ) : null}
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}

export default SearchBar;
