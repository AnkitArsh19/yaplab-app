import React, { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useDimensions } from '../../utils/use-dimensions';
import UserInfoModal from '../modals/UserInfoModal';
import UserSettingsModal from '../modals/UserSettingsModal';
import AboutProjectModal from '../modals/AboutProjectModal';
import '../../styles/SettingsMenu.css';

function SettingsMenu({ onLogout, onCreateGroup, currentUser, onUserUpdate }) {
    const [isOpen, setIsOpen] = useState(false);
    const [showUserInfoModal, setShowUserInfoModal] = useState(false);
    const [showUserSettingsModal, setShowUserSettingsModal] = useState(false);
    const [showAboutProjectModal, setShowAboutProjectModal] = useState(false);
    const containerRef = useRef(null);
    const { height } = useDimensions(containerRef);

    const handleItemClick = (action) => {
        setIsOpen(false);
        
        switch(action) {
            case 'logout':
                onLogout();
                break;
            case 'editProfile':
                setShowUserInfoModal(true);
                break;
            case 'createGroup':
                if (onCreateGroup) {
                    onCreateGroup();
                }
                break;
            case 'settings':
                setShowUserSettingsModal(true);
                break;
            case 'aboutProject':
                setShowAboutProjectModal(true);
                break;
            default:
                break;
        }
    };

    const handleUserInfoSave = async (updatedUser) => {
        if (onUserUpdate) {
            try {
                await onUserUpdate(updatedUser);
                setShowUserInfoModal(false);
            } catch (error) {
                throw error;
            }
        }
    };

    const handleUserSettingsSave = async (saveData) => {
        if (onUserUpdate && saveData.action === 'updateAccount') {
            try {
                await onUserUpdate(saveData.data);
            } catch (error) {
                throw error;
            }
        }
    };

    const handleCloseUserInfoModal = () => {
        setShowUserInfoModal(false);
    };

    const handleCloseUserSettingsModal = () => {
        setShowUserSettingsModal(false);
    };

    const handleCloseAboutProjectModal = () => {
        setShowAboutProjectModal(false);
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
        <div className="settings-container">
            <motion.nav
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

            {showUserInfoModal && currentUser && createPortal(
                <UserInfoModal
                    isOpen={showUserInfoModal}
                    onClose={handleCloseUserInfoModal}
                    user={currentUser}
                    mode="edit"
                    onSave={handleUserInfoSave}
                />,
                document.body
            )}

            {showUserSettingsModal && currentUser && createPortal(
                <UserSettingsModal
                    isOpen={showUserSettingsModal}
                    onClose={handleCloseUserSettingsModal}
                    currentUser={currentUser}
                    onSave={handleUserSettingsSave}
                />,
                document.body
            )}

            {showAboutProjectModal && createPortal(
                <AboutProjectModal
                    isOpen={showAboutProjectModal}
                    onClose={handleCloseAboutProjectModal}
                />,
                document.body
            )}
        </div>
    );
}

const menuItems = [
    { id: 0, label: 'Edit Profile', action: 'editProfile', icon: '/user-pen-solid.svg'},
    { id: 1, label: 'Create Group', action: 'createGroup', icon: '/user-group-solid.svg'},
    { id: 2, label: 'Settings', action: 'settings', icon: '/gear-solid.svg'},
    { id: 3, label: 'About Yaplab', action: 'aboutProject', icon: '/info-solid.svg'},
    { id: 4, label: 'Logout', action: 'logout', icon: '/door-open-solid.svg' }
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
        {menuItems.map((item) => (
            <MenuItem 
                key={item.id}
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

const MenuItem = ({ item, onClick }) => {
    return (
        <motion.li
            className={`settings-list-item ${item.action === 'logout' ? 'logout-item' : ''}`}
            variants={itemVariants}
            whileHover={{ scale: 1.05 }} 
            whileTap={{ scale: 0.95 }}
            onClick={onClick}
        >
            {item.icon && (
                <img
                    src={item.icon}
                    alt=""
                    className="settings-menu-icon"
                    style={{ width: 18, height: 18, marginRight: 12 }}
                />
            )}
            <div className="settings-text-placeholder">
                {item.label}
            </div>
        </motion.li>
    );
};

const Path = (props) => (
    <motion.path
        strokeWidth="3"
        stroke="#a27f68"
        strokeLinecap="round"
        {...props}
    />
);

const MenuToggle = ({ toggle }) => (
    <button className="settings-toggle" onClick={toggle}>
        <svg width="23" height="23" viewBox="0 0 23 23">
            <Path
                variants={{
                    closed: { d: "M 3 5.5 L 20 5.5" },
                    open: { d: "M 3 16.5 L 17 2.5" },
                }}
            />
            <Path
                d="M 3 11.5 L 20 11.5"
                variants={{
                    closed: { opacity: 1 },
                    open: { opacity: 0 },
                }}
                transition={{ duration: 0.1 }}
            />
            <Path
                variants={{
                    closed: { d: "M 3 17.5 L 20 17.5" },
                    open: { d: "M 3 2.5 L 17 16.346" },
                }}
            />
        </svg>
    </button>
);

export default SettingsMenu;
