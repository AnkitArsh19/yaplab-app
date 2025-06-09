import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../Style/SettingsMenu.css';

// Custom hook for dimensions - matches the example code
const useDimensions = (ref) => {
    const dimensions = useRef({ width: 0, height: 0 });

    useEffect(() => {
        if (ref.current) {
            dimensions.current.width = ref.current.offsetWidth;
            dimensions.current.height = ref.current.offsetHeight;
        }
    }, [ref]);

    return dimensions.current;
};

function SettingsMenu({ onLogout }) {
    const [isOpen, setIsOpen] = useState(false);
    const containerRef = useRef(null);
    const { height } = useDimensions(containerRef);

    const handleItemClick = (action) => {
        console.log(`${action} clicked`);
        setIsOpen(false);
        
        switch(action) {
            case 'logout':
                onLogout();
                break;
            case 'editProfile':
                console.log('Edit profile clicked');
                break;
            case 'changeStatus':
                console.log('Change status clicked');
                break;
            case 'createGroup':
                console.log('Create group clicked');
                break;
            case 'settings':
                console.log('Settings clicked');
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

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div className="settings-container">            <motion.nav
                initial={false}
                animate={isOpen ? "open" : "closed"}
                custom={height}
                ref={containerRef}
                className="settings-nav"
            >
                <motion.div className="settings-background" variants={sidebarVariants} />
                
                <AnimatePresence>
                    {isOpen && <Navigation onItemClick={handleItemClick} />}
                </AnimatePresence>
                
                <MenuToggle toggle={() => setIsOpen(!isOpen)} />
            </motion.nav>
        </div>
    );
}

const menuItems = [
    { id: 0, label: 'Edit Profile', action: 'editProfile'},
    { id: 1, label: 'Change Status', action: 'changeStatus'},
    { id: 2, label: 'Create Group', action: 'createGroup'},
    { id: 3, label: 'Settings', action: 'settings'},
    { id: 4, label: 'Logout', action: 'logout' }
];

const sidebarVariants = {
    open: (height = 1000) => ({
        clipPath: `circle(${height * 2 + 200}px at 25px 25px)`,
        transition: {
            type: "spring",
            stiffness: 20,
            restDelta: 2,
            damping: 35, 
        },
    }),
    closed: {
        clipPath: "circle(20px at 25px 25px)",
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

const Navigation = ({ onItemClick }) => (
    <motion.ul 
        className="settings-list" 
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
    return (
        <motion.li
            className={`settings-list-item ${item.action === 'logout' ? 'logout-item' : ''}`}
            variants={itemVariants}
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={onClick}
        >
            <div className="settings-text-placeholder">
                {item.label}
            </div>
        </motion.li>
    );
};

const Path = (props) => (
    <motion.path
        strokeWidth="3"
        stroke="hsl(0, 0%, 18%)"
        strokeLinecap="round"
        {...props}
    />
);

const MenuToggle = ({ toggle }) => (
    <button className="settings-toggle" onClick={toggle}>
        <svg width="23" height="23" viewBox="0 0 23 23">
            <Path
                variants={{
                    closed: { d: "M 2 2.5 L 20 2.5" },
                    open: { d: "M 3 16.5 L 17 2.5" },
                }}
            />
            <Path
                d="M 2 9.423 L 20 9.423"
                variants={{
                    closed: { opacity: 1 },
                    open: { opacity: 0 },
                }}
                transition={{ duration: 0.1 }}
            />
            <Path
                variants={{
                    closed: { d: "M 2 16.346 L 20 16.346" },
                    open: { d: "M 3 2.5 L 17 16.346" },
                }}
            />
        </svg>
    </button>
);

export default SettingsMenu;