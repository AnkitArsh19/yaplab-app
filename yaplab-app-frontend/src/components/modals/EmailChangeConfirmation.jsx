import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import apiClient from '../../utils/apiClient';
import '../../styles/EmailChangeConfirmation.css';

const EmailChangeConfirmation = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [status, setStatus] = useState('loading'); // loading, success, error
    const [message, setMessage] = useState('');

    useEffect(() => {
        const confirmEmailChange = async () => {
            const token = searchParams.get('token');
            
            if (!token) {
                setStatus('error');
                setMessage('Invalid or missing token');
                return;
            }

            try {
                const response = await apiClient.post('/auth/confirm-email-change', null, {
                    params: { token }
                });

                if (response.ok) {
                    const result = await response.json();
                    setStatus('success');
                    setMessage(result.message || 'Email address updated successfully!');
                } else {
                    const errorData = await response.json();
                    setStatus('error');
                    setMessage(errorData.error || 'Failed to confirm email change');
                }
            } catch (error) {
                console.error('Error confirming email change:', error);
                setStatus('error');
                setMessage('An error occurred while confirming email change');
            }
        };

        confirmEmailChange();
    }, [searchParams]);

    const handleGoToLogin = () => {
        navigate('/login');
    };

    return (
        <div className="email-confirmation-container">
            <div className="email-confirmation-card">
                {status === 'loading' && (
                    <>
                        <div className="loading-spinner"></div>
                        <h2 className="title loading">
                            Confirming Email Change...
                        </h2>
                        <p className="message">
                            Please wait while we verify your email change request.
                        </p>
                    </>
                )}

                {status === 'success' && (
                    <>
                        <div className="status-icon success">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3">
                                <polyline points="20,6 9,17 4,12"></polyline>
                            </svg>
                        </div>
                        <h2 className="title success">
                            Email Updated Successfully!
                        </h2>
                        <p className="message">
                            {message}
                        </p>
                        <button
                            onClick={handleGoToLogin}
                            className="button success"
                        >
                            Continue to Login
                        </button>
                    </>
                )}

                {status === 'error' && (
                    <>
                        <div className="status-icon error">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3">
                                <circle cx="12" cy="12" r="10"></circle>
                                <line x1="15" y1="9" x2="9" y2="15"></line>
                                <line x1="9" y1="9" x2="15" y2="15"></line>
                            </svg>
                        </div>
                        <h2 className="title error">
                            Email Change Failed
                        </h2>
                        <p className="message">
                            {message}
                        </p>
                        <button
                            onClick={handleGoToLogin}
                            className="button error"
                        >
                            Go to Login
                        </button>
                    </>
                )}
            </div>
        </div>
    );
};

export default EmailChangeConfirmation;
