import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../../styles/AttachmentMenu.css';
import { useDimensions } from '../../utils/use-dimensions';

function AttachmentMenu({ onFileSelect }) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef(null);
    const { height } = useDimensions(containerRef);

    const handleItemClick = (action) => {
        setIsOpen(false);
        
        const input = document.createElement('input');
        input.type = 'file';
        input.style.display = 'none';

        switch(action) {
            case 'image':
                input.accept = 'image/*';
                input.multiple = true;
                break;
            case 'video':
                input.accept = 'video/*';
                input.multiple = true;
                break;
            case 'audio':
                input.accept = 'audio/*';
                input.multiple = true;
                break;
            case 'document':
                input.accept = '.pdf,.doc,.docx,.txt,.rtf,.odt,.xls,.xlsx,.ppt,.pptx';
                input.multiple = true;
                break;
            default:
                return;
        }
        
        input.onchange = (e) => {
            const files = Array.from(e.target.files);
            if (files.length > 0) {
                const totalSize = files.reduce((sum, file) => sum + file.size, 0);
                if (totalSize > 50 * 1024 * 1024) {
                    alert('Total file size must be less than 50MB');
                    return;
                }
                
                const oversizedFiles = files.filter(file => file.size > 50 * 1024 * 1024);
                if (oversizedFiles.length > 0) {
                    alert('File size must be less than 50MB');
                    return;
                }
                
                if (onFileSelect) {
                    onFileSelect(files, action);
                }
            }
            document.body.removeChild(input);
        };
        
        document.body.appendChild(input);
        input.click();
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

    const toggleMenu = () => {
        setIsOpen(!isOpen);
    };

    return (
        <div className="attachment-container">
            <motion.nav
                initial={false}
                animate={isOpen ? "open" : "closed"}
                custom={height}
                ref={containerRef}
                className="attachment-nav"
            >
                <motion.div 
                    className="attachment-background" 
                    variants={backgroundVariants}
                    initial="closed"
                    animate={isOpen ? "open" : "closed"}
                    transition={{ duration: 0.2 }}
                >
                    <AnimatePresence>
                        {isOpen && <Navigation onItemClick={handleItemClick} />}
                    </AnimatePresence>
                </motion.div>
                
                <AttachmentToggle toggle={toggleMenu} isOpen={isOpen} />
            </motion.nav>
        </div>
    );
}

const menuItems = [
    { id: 0, label: 'Image', action: 'image', icon: '/image-solid.svg' },
    { id: 1, label: 'Video', action: 'video', icon: '/video-solid.svg' },
    { id: 2, label: 'Audio', action: 'audio', icon: '/headphones-solid.svg' },
    { id: 3, label: 'Document', action: 'document', icon: '/file-solid.svg' }
];

const backgroundVariants = {
    open: (height = 1000) => ({
        clipPath: `circle(${height * 2 + 200}px at 50% calc(100% + 20px))`,
        transition: {
            type: "spring",
            stiffness: 20,
            restDelta: 2,
            damping: 35,
        },
    }),
    closed: {
        clipPath: "circle(0px at 50% calc(100% + 20px))",
        transition: {
            delay: 0.2,
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

const Navigation = ({ onItemClick }) => (
    <motion.ul
        className="attachment-list"
        variants={navVariants}
        initial="closed"
        animate="open"
        exit="closed"
    >
        <div className="attachment-header">
            <p className="attachment-limit-text">Upload files up to 50MB</p>
        </div>
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

const itemVariants = {
    open: {
        y: 0,
        opacity: 1, // Always visible
        transition: {
            y: { stiffness: 1000, velocity: -100 },
        },
    },
    closed: {
        y: 50,
        opacity: 1, // Always visible
        transition: {
            y: { stiffness: 1000 },
        },
    },
};

const MenuItem = ({ i, item, onClick }) => {
    return (
        <motion.li
            className="attachment-list-item"
            variants={itemVariants}
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={onClick}
        >
            <div className="attachment-icon-placeholder">
                <img src={item.icon} alt={item.label} className="attachment-menu-icon" />
            </div>
            <div className="attachment-text-placeholder">
                {item.label}
            </div>
        </motion.li>
    );
};

const AttachmentToggle = ({ toggle, isOpen }) => (
    <button className={`attachment-toggle ${isOpen ? 'open' : ''}`} onClick={toggle}>
        <img src="/attachment-icon.svg" alt="Attachment" className="attachment-toggle-icon" />
    </button>
);

export default AttachmentMenu;