"use client";

import { motion } from "framer-motion";

function LoadingThreeDots({ color = "#a27f68", size = 18 }) {
    const dotVariants = {
        jump: {
            y: [-2, -8, -2],
            transition: {
                duration: 0.6,
                repeat: Infinity,
                repeatType: "loop",
                ease: "easeInOut",
            },
        },
    };

    return (
        <motion.div
            animate="jump"
            transition={{ staggerChildren: 0.2 }}
            className="loading-container"
            style={{ color, fontSize: size }}
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
                height: 100%;
                min-height: 20px;
            }

            .loading-dot {
                width: 7px;
                height: 7px;
                border-radius: 50%;
                background-color: white;
                will-change: transform;
                align-self: center;
            }
            `}
        </style>
    );
}

export default LoadingThreeDots;
