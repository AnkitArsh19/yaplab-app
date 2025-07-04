import React from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import '../../styles/AboutProjectModal.css';

const AboutProjectModal = ({ isOpen, onClose }) => {
    if (!isOpen) return null;

    const handleOverlayClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    return createPortal(
        <AnimatePresence>
            <motion.div
                className="about-project-overlay"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={handleOverlayClick}
            >
                <motion.div
                    className="about-project-modal"
                    initial={{ scale: 0.8, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.8, opacity: 0 }}
                    transition={{ type: "spring", damping: 20, stiffness: 300 }}
                    onClick={(e) => e.stopPropagation()}
                >
                    <div className="about-project-header">
                        <h2 className="about-project-title">About Yaplab</h2>
                        <button
                            className="about-project-close-btn"
                            onClick={onClose}
                            aria-label="Close"
                        >
                            <img src="/cross-icon.png" alt="Close" />
                        </button>
                    </div>

                    <div className="about-project-content">
                        <div className="about-project-logo-section">
                            <img src="/logo.png" alt="Yaplab Logo" className="about-project-logo" />
                            <h3 className="about-project-name">Yaplab</h3>
                        </div>

                        <div className="about-project-sections">
                            <section className="about-section">
                                <div className="section-header">
                                    <img src="/info-solid.svg" alt="" className="section-icon" />
                                    <h4>About Yaplab</h4>
                                </div>
                                <p>
                                    Hey! Welcome to Yaplab, a messaging app I started building around the end of February 2025—and after months of grinding (and far too many long nights), finally brought to life by July.
                                </p>
                                <p>
                                    Now about the name—Yaplab—I wish I had a cooler story, but honestly, it just popped into my head. "Yap" felt casual and chatty (like friends yapping away), and "lab" made it sound like a space to experiment and build. Somehow it stuck. And it kind of works, right?
                                </p>
                            </section>

                            <section className="about-section">
                                <div className="section-header">
                                    <img src="/gear-solid.svg" alt="" className="section-icon" />
                                    <h4>The Journey</h4>
                                </div>
                                <p>
                                    Yaplab started as a simple idea: build a messaging app to learn something new. But like most "simple" projects, it quickly spiraled into a full-blown system with a proper frontend and backend, complete with real-world features:
                                </p>
                                <ul className="feature-list">
                                    <li><img src="/user-group-solid.svg" alt="" className="list-icon" />Personal and group messaging</li>
                                    <li><img src="/circle-solid.svg" alt="" className="list-icon" />Real-time status updates using WebSockets</li>
                                    <li><img src="/emoji-icon.svg" alt="" className="list-icon" />Emojis, GIFs, file and media sharing</li>
                                    <li><img src="/headphones-solid.svg" alt="" className="list-icon" />Voice recorder</li>
                                    <li><img src="/user-pen-solid.svg" alt="" className="list-icon" />Profile pictures, user settings</li>
                                    <li><img src="/square-check-solid.svg" alt="" className="list-icon" />JWT authentication & email-based onboarding</li>
                                    <li><img src="/info-solid.svg" alt="" className="list-icon" />Notifications, message editing, deleting users/groups</li>
                                    <li><img src="/expand-solid.svg" alt="" className="list-icon" />Smooth UI with animations</li>
                                </ul>
                                <p>
                                    …basically, I ended up trying to build something that could actually feel like a real product.
                                </p>
                                <p>
                                    Some days were honestly brutal—fixing tiny bugs that just wouldn't go away, rewriting parts of the codebase because I didn't like how something worked, or just sitting there wondering why WebSockets seemed so determined to ruin my day.
                                </p>
                                <p>But I stuck with it. And slowly, it came together.</p>
                            </section>

                            <section className="about-section">
                                <div className="section-header">
                                    <img src="/search-solid.svg" alt="" className="section-icon" />
                                    <h4>AI, Docs, and All That Jazz</h4>
                                </div>
                                <p>
                                    I used everything I could get my hands on: tutorials, documentation, GitHub issues, Reddit threads, and yes—AI tools. They weren't just there to spit out code; they helped me think through architecture, understand edge cases, and save time when it mattered.
                                </p>
                                <p>
                                    Still, I know that real understanding comes from going deep—so I did fall back on traditional methods too. The process reminded me just how much I don't know, and honestly, that's kind of exciting.
                                </p>
                            </section>

                            <section className="about-section">
                                <div className="section-header">
                                    <img src="/broom-solid.svg" alt="" className="section-icon" />
                                    <h4>Bugs & Incompleteness (Yep, That Too)</h4>
                                </div>
                                <p>
                                    Yaplab is big. Possibly too big for one person to test perfectly. I've tried my best to make sure it works smoothly, but you might still stumble upon a few rough edges—bugs, unexpected behavior, or even missing features.
                                </p>
                                <p>
                                    And if you do? Please let me know! I'd love to improve it and push out fixes. This is all still a work in progress, and feedback means the world.
                                </p>
                                <p>
                                    Also, I had to accept that no project is ever truly "done." There's always more you can add. But at some point, you just have to pause and ship.
                                </p>
                            </section>

                            <section className="about-section">
                                <div className="section-header">
                                    <img src="/square-check-solid.svg" alt="" className="section-icon" />
                                    <h4>The End-to-End Encryption Story</h4>
                                </div>
                                <p>
                                    Let's talk about the one feature I've always wanted to add: end-to-end encryption (E2EE).
                                </p>
                                <p>
                                    It's become such a must-have for messaging apps today, and I was super excited to implement it. But I quickly realized it's not a small feature. It's basically a whole project on its own. There are very few modern, updated open-source libraries out there. Signal has one—but it's pretty outdated (last real update was back in 2019). And integrating it into a Spring Boot-based app like Yaplab would require some serious refactoring and a deep understanding of cryptography.
                                </p>
                                <p>
                                    In short, I hit a wall. And I didn't want to force something I couldn't do properly. So for now, E2EE is on the wishlist, not in the release. Hopefully, someday I'll crack it the right way.
                                </p>
                            </section>

                            <section className="about-section">
                                <div className="section-header">
                                    <img src="/info-solid.svg" alt="" className="section-icon" />
                                    <h4>Final Thoughts</h4>
                                </div>
                                <p>
                                    Honestly? I still can't believe I pulled this off. Yaplab isn't just code—it's weeks of problem-solving, learning, failing, retrying, and finally seeing it all work.
                                </p>
                                <p>
                                    Sure, the UI might not win awards (UI/UX isn't really my comfort zone as a developer), but I gave it all I could—animations, responsiveness, clean visuals. I just wanted it to feel good to use.
                                </p>
                                <p>
                                    And while it may not solve any global problem, this project helped me level up—in architecture, in real-world coding, and in understanding how modern apps are built.
                                </p>
                                <p>
                                    So, thank you for trying Yaplab. If you spot anything broken, or just want to share thoughts, I'd love to hear from you. Every bit of feedback helps me grow.
                                </p>
                            </section>
                        </div>

                        <div className="about-project-footer">
                            <div className="social-links">
                                <a
                                    href="https://github.com/AnkitArsh19/yaplab-app"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="social-link"
                                    aria-label="GitHub Repository"
                                >
                                    <img src="/github-brands.svg" alt="GitHub" />
                                    <span>View on GitHub</span>
                                </a>
                                <a
                                    href="https://www.linkedin.com/in/ankitarsh19/"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="social-link"
                                    aria-label="LinkedIn Profile"
                                >
                                    <img src="/linkedin-brands.svg" alt="LinkedIn" />
                                    <span>Connect on LinkedIn</span>
                                </a>
                            </div>
                        </div>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>,
        document.body
    );
};

export default AboutProjectModal;
