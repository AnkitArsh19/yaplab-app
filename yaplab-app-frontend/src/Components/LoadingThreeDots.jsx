"use client";

import { motion } from "framer-motion";

function LoadingThreeDots() {
    const dotVariants = {
        jump: {
            y: -10,
            transition: {
                duration: 0.5,
                repeat: Infinity,
                repeatType: "mirror",
                ease: "easeInOut",
            },
        },
    };

    return (
        <motion.div
            animate="jump"
            transition={{ staggerChildren: 0.2 }}
            className="loading-container"
        >
            <motion.div className="loading-dot" variants={dotVariants} />
            <motion.div className="loading-dot" variants={dotVariants} />
            <motion.div className="loading-dot" variants={dotVariants} />
            <StyleSheet />
        </motion.div>
    );
}

function StyleSheet() {
    return (
        <style>
            {`
            .loading-container {
                display: flex;
                justify-content: center;
                align-items: center;
                gap: 8px;
            }

            .loading-dot {
                width: 7px;
                height: 7px;
                border-radius: 50%;
                background-color: white;
                will-change: transform;
            }
            `}
        </style>
    );
}

export default LoadingThreeDots;