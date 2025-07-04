import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useDimensions } from '../../utils/use-dimensions';
import '../../styles/ChatMenu.css';

function ChatMenu({ onAction, chatType = 'personal', isGroupCreator = false }) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef(null);
    const { height } = useDimensions(containerRef);

    const handleItemClick = (action) => {
        setIsOpen(false);
        
        if (onAction) {
            onAction(action);
        }
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div className="chat-menu-container">
            <motion.nav
                initial={false}
                animate={isOpen ? "open" : "closed"}
                custom={height}
                ref={containerRef}
                className="chat-menu-nav"
            >
                <AnimatePresence>
                    {isOpen && (
                        <motion.div
                            className="chat-menu-popup"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            exit={{ opacity: 0 }}
                        >
                            <motion.div className="chat-menu-background" variants={menuVariants} />
                            <Navigation 
                                onItemClick={handleItemClick} 
                                chatType={chatType}
                                isGroupCreator={isGroupCreator}
                            />
                        </motion.div>
                    )}
                </AnimatePresence>
                
                <MenuToggle toggle={() => setIsOpen(!isOpen)} isOpen={isOpen} />
            </motion.nav>
        </div>
    );
}

const menuIcons = {
    info: '/info-solid.svg',
    search: '/search-solid.svg',
    clear: '/broom-solid.svg',
    settings: '/gear-solid.svg',
    delete: '/delete-icon.svg',
    leave: '/door-open-solid.svg'
};

const getMenuItems = (chatType, isGroupCreator) => {
    const baseItems = [
        { id: 1, label: `${chatType === 'group' ? 'Group' : 'Contact'} Info`, action: chatType === 'group' ? 'groupInfo' : 'contactInfo', icon: 'info' },
        { id: 2, label: 'Search Messages', action: 'searchMessages', icon: 'search' },
        { id: 3, label: 'Clear Chat', action: 'clearChat', icon: 'clear' }
    ];

    if (chatType === 'group') {
        return [
            ...baseItems,
            ...(isGroupCreator 
                ? [
                    { id: 4, label: 'Group Settings', action: 'groupSettings', icon: 'settings' },
                    { id: 5, label: 'Delete Group', action: 'deleteGroup', icon: 'delete' }
                  ]
                : [{ id: 4, label: 'Leave Group', action: 'leaveGroup', icon: 'leave' }]
            )
        ];
    }

    return [
        ...baseItems,
        { id: 4, label: 'Delete Chat', action: 'deleteChat', icon: 'delete' }
    ];
};

const menuVariants = {
    open: (height = 1000) => ({
        clipPath: `circle(${height * 2 + 200}px at calc(100% - 25px) 25px)`,
        transition: {
            type: "spring",
            stiffness: 20,
            restDelta: 2,
            damping: 35, 
        },
    }),
    closed: {
        clipPath: "circle(20px at calc(100% - 25px) 25px)",
        transition: {
            delay: 0.2,
            type: "spring",
            stiffness: 400,
            damping: 50,
        },
    },
};

const navVariants = {
    open: {
        transition: { staggerChildren: 0.07, delayChildren: 0.2 },
    },
    closed: {
        transition: { staggerChildren: 0.05, staggerDirection: -1 },
    },
};

const Navigation = ({ onItemClick, chatType, isGroupCreator }) => {
    const menuItems = getMenuItems(chatType, isGroupCreator);
    
    return (
        <motion.ul 
            className="chat-menu-list" 
            variants={navVariants}
            initial="closed"
            animate="open"
            exit="closed"
        >
            {menuItems.map((item, i) => (
                <MenuItem 
                    key={item.id}
                    i={i} 
                    item={item}
                    onClick={() => onItemClick(item.action)} 
                />
            ))}
        </motion.ul>
    );
};

const itemVariants = {
    open: {
        y: 0,
        opacity: 1,
        transition: {
            y: { stiffness: 1000, velocity: -100 },
        },
    },
    closed: {
        y: 50,
        opacity: 0,
        transition: {
            y: { stiffness: 1000 },
        },
    },
};

const MenuItem = ({ i, item, onClick }) => {
    const isDangerAction = ['deleteChat', 'deleteGroup', 'leaveGroup'].includes(item.action);
    const iconSrc = menuIcons[item.icon];
    
    return (
        <motion.li
            className={`chat-menu-list-item ${isDangerAction ? 'danger-item' : ''}`}
            variants={itemVariants}
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={onClick}
        >
            {iconSrc && (
                <img
                    src={iconSrc}
                    alt=""
                    className="chat-menu-icon"
                    style={{ width: 20, height: 20, marginRight: 12 }}
                />
            )}
            <div className="chat-menu-text-placeholder">{item.label}</div>
        </motion.li>
    );
};

const MenuToggle = ({ toggle, isOpen }) => (
    <button className="chat-menu-toggle" onClick={toggle}>
        <div className={`menu-dots-container ${isOpen ? 'open' : 'closed'}`}>
            <div className="dot dot-1"></div>
            <div className="dot dot-2"></div>
            <div className="dot dot-3"></div>
            <div className="dot dot-4"></div>
        </div>
    </button>
);

export default ChatMenu;
