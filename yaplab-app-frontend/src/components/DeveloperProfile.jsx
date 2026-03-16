import React from 'react';
import '../styles/DeveloperProfile.css';
import ankitImg from '../assets/ankit.jpg';

const DeveloperProfile = () => {
    return (
        <div className="container dev-profile-container">
            <div className="image-container">
            </div>
            
            <div className="Form dev-profile-form">
                <div className="auth-logo">
                    <img src={ankitImg} alt="Ankit Arsh" className="profile-image" />
                </div>
                
                <h1 className="Heading dev-heading">Developer Profile</h1>
                
                <div className="profile-details">
                    <div className="detail-item">
                        <span className="detail-label">Name:</span>
                        <span className="detail-value">Ankit Arsh</span>
                    </div>
                    <div className="detail-item">
                        <span className="detail-label">Roll No:</span>
                        <span className="detail-value">2306184</span>
                    </div>
                    <div className="detail-item">
                        <span className="detail-label">University:</span>
                        <span className="detail-value">KIIT University</span>
                    </div>
                    <div className="detail-item">
                        <span className="detail-label">Branch:</span>
                        <span className="detail-value">Information Technology</span>
                    </div>
                    <div className="detail-item">
                        <span className="detail-label">Section:</span>
                        <span className="detail-value">IT02</span>
                    </div>
                    <div className="detail-item">
                        <span className="detail-label">Semester:</span>
                        <span className="detail-value">6th</span>
                    </div>
                    <div className="detail-item">
                        <span className="detail-label">School:</span>
                        <span className="detail-value">School of Computer Engineering</span>
                    </div>
                </div>
                
                <div className="dev-footer">
                    <button 
                        className="submitButton back-button" 
                        onClick={() => window.history.back()}
                    >
                        Back
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DeveloperProfile;
