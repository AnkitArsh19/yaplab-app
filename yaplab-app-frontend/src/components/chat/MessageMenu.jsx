import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../../styles/MessageMenu.css';
import { useDimensions } from '../../utils/use-dimensions';

function MessageMenu({ isOwnMessage, onReply, onCopy, onForward, onEdit, onDelete, onSelect, setIsAnyMenuOpen, canEdit, isSelectionMode }) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef(null);
    const { height } = useDimensions(containerRef);

    const handleItemClick = (action) => {
        setIsOpen(false);
        
        switch(action) {
            case 'reply':
                onReply && onReply();
                break;
            case 'copy':
                onCopy && onCopy();
                break;
            case 'forward':
                onForward && onForward();
                break;
            case 'select':
                onSelect && onSelect();
                break;
            case 'edit':
                onEdit && onEdit();
                break;
            case 'delete':
                onDelete && onDelete();
                break;
            default:
                break;
        }
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (containerRef.current && !containerRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);

    useEffect(() => {
        if (setIsAnyMenuOpen) {
            setIsAnyMenuOpen(isOpen);
        }
    }, [isOpen, setIsAnyMenuOpen]);

    const toggleMenu = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setIsOpen(!isOpen);
    };

    if (isSelectionMode) {
        return null;
    }

    return (
        <div className="message-menu-container">
            <motion.nav
                initial={false}
                animate={isOpen ? "open" : "closed"}
                custom={height}
                ref={containerRef}
                className="message-menu-nav"
            >
                <motion.div 
                    className="message-menu-background" 
                    variants={backgroundVariants}
                    initial="closed"
                    animate={isOpen ? "open" : "closed"}
                >
                    <AnimatePresence>
                        {isOpen && <Navigation onItemClick={handleItemClick} isOwnMessage={isOwnMessage} canEdit={canEdit} />}
                    </AnimatePresence>
                </motion.div>
                
                <MessageToggle toggle={toggleMenu} isOpen={isOpen} />
            </motion.nav>
        </div>
    );
}

const menuItems = [
    { id: 0, label: 'Reply', action: 'reply', icon: '/reply-solid.svg' },
    { id: 1, label: 'Copy', action: 'copy', icon: '/copy-solid.svg' },
    { id: 2, label: 'Forward', action: 'forward', icon: '/share-solid.svg' },
    { id: 3, label: 'Select', action: 'select', icon: '/square-check-solid.svg' }
];

const ownerMenuItems = [
    { id: 4, label: 'Edit', action: 'edit', icon: '/pen-solid.svg' },
    { id: 5, label: 'Delete', action: 'delete', icon: '/delete-icon.svg' }
];

const backgroundVariants = {
    open: (height = 1000) => ({
        clipPath: `circle(${height * 2 + 200}px at 50% 50%)`,
        transition: {
            type: "spring",
            stiffness: 20,
            restDelta: 2,
            damping: 35,
        },
    }),
    closed: {
        clipPath: "circle(0px at 50% 50%)",
        transition: {
            type: "spring",
            stiffness: 400,
            damping: 50,
        }
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

const Navigation = ({ onItemClick, isOwnMessage, canEdit }) => {
    const filteredOwnerItems = ownerMenuItems.filter(item => {
        if (item.action === 'edit') {
            return canEdit;
        }
        return true;
    });
    
    const allItems = isOwnMessage ? [...menuItems, ...filteredOwnerItems] : menuItems;
    
    return (
        <motion.ul 
            className="message-menu-list" 
            variants={navVariants}
            initial="closed"
            animate="open"
            exit="closed"
        >
            {allItems.map((item) => (
                <MenuItem 
                    key={item.id}
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

const MenuItem = ({ item, onClick }) => {
    const isDelete = item.label === 'Delete';
    
    return (
        <motion.li
            className={`message-menu-list-item ${isDelete ? 'delete-item' : ''}`}
            variants={itemVariants}
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={onClick}
        >
            <div className="message-menu-icon-placeholder">
                <img 
                    src={item.icon}
                    alt={item.label} 
                    className="message-menu-icon"
                />
            </div>
            <div 
                className="message-menu-text-placeholder"
                style={isDelete ? { color: '#dc3545' } : {}}
            >
                {item.label}
            </div>
        </motion.li>
    );
};

const MessageToggle = ({ toggle, isOpen }) => (
    <button 
        className={`message-toggle ${isOpen ? 'open' : ''}`} 
        onClick={toggle}
    >
        <img src="/chevron-down-solid.svg" alt="Menu" className="message-toggle-icon" />
    </button>
);

export default MessageMenu;