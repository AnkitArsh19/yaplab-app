import React, { useState, useEffect } from "react";
import "../../styles/AuthPage.css";
import { motion } from "framer-motion";
import LoadingThreeDots from "../ui/LoadingThreeDots";
import { useNavigate, useSearchParams } from "react-router-dom";
import apiClient from '../../utils/apiClient.js';

function AuthPage({onLoginSuccess}) {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const resetToken = searchParams.get('token');
    
    const [signUpDetails, setSignUpDetails] = useState({
        userName: "",
        emailId: "",
        mobileNumber: "",
        password: "",
    });

    const [logInDetails, setLogInDetails] = useState({
        emailId: "",
        password: "",
    });

    const [forgotPswdDetails, setForgotPswdDetails] = useState({
        emailId: "",
    });

    const [resetPasswordDetails, setResetPasswordDetails] = useState({
        newPassword: "",
        confirmPassword: "",
    });

    const [isLoading, setIsLoading] = useState(false);
    const [isLogin, setIsLogin] = useState(true);
    const [isSignUp, setIsSignUp] = useState(false);
    const [isForgotPassword, setIsForgotPassword] = useState(false);
    const [isResetPassword, setIsResetPassword] = useState(false);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [message, setMessage] = useState("");
    useEffect(() => {
        if (resetToken) {
            setIsLogin(false);
            setIsSignUp(false);
            setIsForgotPassword(false);
            setIsResetPassword(true);
            setMessage("");
        }
    }, [resetToken]);

    function changeLogin() {
        if (isLogin) {
            setIsLogin(false);
            setIsSignUp(true);
            setIsForgotPassword(false);
            setIsResetPassword(false);
            setSignUpDetails({ userName: "", emailId: "", mobileNumber: "", password: "" });
        } else {
            setIsLogin(true);
            setIsSignUp(false);
            setIsForgotPassword(false);
            setIsResetPassword(false);
            setLogInDetails({ emailId: "", password: "" });
        }
        setIsSubmitted(false);
        setMessage("");
    }

    const handleForgotPassword = () => {
        setIsLogin(false);
        setIsSignUp(false);
        setIsForgotPassword(true);
        setIsResetPassword(false);
        setForgotPswdDetails({ emailId: "" });
        setIsSubmitted(false);
        setMessage("");
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        if (isLogin) {
            setLogInDetails((prev) => ({ ...prev, [name]: value }));
        } else if (isSignUp) {
            setSignUpDetails((prev) => ({ ...prev, [name]: value }));
        } else if (isForgotPassword) {
            setForgotPswdDetails((prev) => ({ ...prev, [name]: value }));
        } else if (isResetPassword) {
            setResetPasswordDetails((prev) => ({ ...prev, [name]: value }));
        }
    };

    const handleSubmitForm = async (e) => {
        e.preventDefault();
        setIsSubmitted(true);
        setMessage("");

        if (isLogin) {
            if (!logInDetails.emailId || !logInDetails.password) {
                setMessage("Please fill all the fields.");
                return;
            }
        } else if (isSignUp) {
            if (!signUpDetails.userName || !signUpDetails.emailId || !signUpDetails.mobileNumber || !signUpDetails.password) {
                setMessage("Please fill all the fields.");
                return;
            }
        } else if (isForgotPassword) {
            if (!forgotPswdDetails.emailId) {
                setMessage("Please fill all the fields.");
                return;
            }
        } else if (isResetPassword) {
            if (!resetPasswordDetails.newPassword || !resetPasswordDetails.confirmPassword) {
                setMessage("Please fill all the fields.");
                return;
            }
            if (resetPasswordDetails.newPassword !== resetPasswordDetails.confirmPassword) {
                setMessage("Passwords do not match.");
                return;
            }
            if (resetPasswordDetails.newPassword.length < 6) {
                setMessage("Password must be at least 6 characters long.");
                return;
            }
        }

        setIsLoading(true);
        try {
            let response;
            if (isLogin) {
                response = await apiClient.post("/auth/login", logInDetails);
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    
                    if (response.status === 401) {
                        setMessage("Incorrect email or password. Please try again.");
                    } else if (response.status === 400) {
                        if (errorData.status === 'EMAIL_NOT_VERIFIED_RESENT') {
                            setMessage("Email not verified. A verification email has been sent to your inbox.");
                        } else if (errorData.status === 'EMAIL_NOT_VERIFIED') {
                            setMessage("Email not verified. Please check your inbox or request a new verification email.");
                        } else {
                            setMessage(errorData.message || "Login failed. Please try again.");
                        }
                    } else if (response.status === 500) {
                        setMessage("Server error. Please try again later.");
                    } else {
                        setMessage(errorData.message || "Login failed. Please try again.");
                    }
                    return;
                }
                
                const data = await response.json();
                onLoginSuccess(data);
                navigate("/chat");
                
            } else if (isSignUp) {
                response = await apiClient.post("/auth/register", signUpDetails);
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    setMessage(errorData.message || "Registration failed. Please try again.");
                    return;
                }
                
                const data = await response.json();
                setSignUpDetails({ userName: "", emailId: "", mobileNumber: "", password: "" });
                setMessage(data.message || "Registration successful! Please check your email to verify your account.");
                
            } else if (isForgotPassword) {
                response = await apiClient.post("/auth/forgot-password", forgotPswdDetails);
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    setMessage(errorData.message || "Failed to send reset email. Please try again.");
                    return;
                }
                
                setForgotPswdDetails({ emailId: "" });
                setMessage("Password reset link sent to your email!");
                
            } else if (isResetPassword) {
                response = await apiClient.post("/auth/reset-password", {
                    token: resetToken,
                    newPassword: resetPasswordDetails.newPassword
                });
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    if (response.status === 400) {
                        setMessage(errorData.message || "Invalid or expired reset token.");
                    } else {
                        setMessage("Failed to reset password. Please try again.");
                    }
                    return;
                }
                
                setMessage("Password reset successfully! You can now login with your new password.");
                setTimeout(() => {
                    navigate("/", { replace: true });
                    setIsResetPassword(false);
                    setIsLogin(true);
                    setMessage("");
                }, 2000);
            }
        } catch (error) {
            console.error("Error during form submission:", error);
            if (error.message.includes('Failed to fetch') || error.name === 'TypeError') {
                setMessage("Network error or server unavailable. Please try again later.");
            } else {
                setMessage("An unexpected error occurred. Please try again.");
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <>
            <div className="container">
                <div className="image-container">
                    <img className="shape" src="shapes.png" alt="Auth page" />
                </div>
                <div className="Form">
                    <div className="auth-logo">
                        <img className="auth-logo-image" src="logo-name.png" alt="logo" />
                    </div>
                    <p className="Heading">
                        {isLogin ? "Login" : 
                         isSignUp ? "Sign up" : 
                         isForgotPassword ? "Reset Password" :
                         isResetPassword ? "Set New Password" : "Login"}
                    </p>
                    
                    <form onSubmit={handleSubmitForm}>
                        <div className="form-content">
                            {isSignUp && (
                                <>
                                    <input
                                        type="text"
                                        className="input_username"
                                        placeholder="Enter your name"
                                        name="userName"
                                        value={signUpDetails.userName}
                                        onChange={handleChange}
                                    />
                                    <br />
                                    <input
                                        type="email"
                                        className="input_emailid"
                                        placeholder="Enter your Email Id"
                                        name="emailId"
                                        value={signUpDetails.emailId}
                                        onChange={handleChange}
                                    />
                                    <br />
                                    <input
                                        type="text"
                                        className="input_mobilenumber"
                                        placeholder="Enter your Mobile Number"
                                        name="mobileNumber"
                                        value={signUpDetails.mobileNumber}
                                        onChange={handleChange}
                                    />
                                    <br />
                                    <div className="passwordfield">
                                        <input
                                            type={showPassword ? "text" : "password"}
                                            className="input_password"
                                            placeholder="Enter Password"
                                            name="password"
                                            value={signUpDetails.password}
                                            onChange={handleChange}
                                        />
                                        <div className="eye-container">
                                            <img
                                                className="eye"
                                                src={showPassword ? "eye.svg" : "eye-closed.svg"}
                                                alt={showPassword ? "Hide password" : "Show password"}
                                                onClick={() => setShowPassword((prev) => !prev)}
                                            />
                                        </div>
                                    </div>
                                </>
                            )}
                            
                            {isForgotPassword && (
                                <input
                                    type="email"
                                    className="input_emailid"
                                    placeholder="Enter your Email Id"
                                    name="emailId"
                                    value={forgotPswdDetails.emailId}
                                    onChange={handleChange}
                                />
                            )}

                            {isResetPassword && (
                                <>
                                    <div className="passwordfield">
                                        <input
                                            type={showPassword ? "text" : "password"}
                                            className="input_password"
                                            placeholder="Enter New Password"
                                            name="newPassword"
                                            value={resetPasswordDetails.newPassword}
                                            onChange={handleChange}
                                        />
                                        <div className="eye-container">
                                            <img
                                                className="eye"
                                                src={showPassword ? "eye.svg" : "eye-closed.svg"}
                                                alt={showPassword ? "Hide password" : "Show password"}
                                                onClick={() => setShowPassword((prev) => !prev)}
                                            />
                                        </div>
                                    </div>
                                    <br />
                                    <div className="passwordfield">
                                        <input
                                            type={showConfirmPassword ? "text" : "password"}
                                            className="input_password"
                                            placeholder="Confirm New Password"
                                            name="confirmPassword"
                                            value={resetPasswordDetails.confirmPassword}
                                            onChange={handleChange}
                                        />
                                        <div className="eye-container">
                                            <img
                                                className="eye"
                                                src={showConfirmPassword ? "eye.svg" : "eye-closed.svg"}
                                                alt={showConfirmPassword ? "Hide password" : "Show password"}
                                                onClick={() => setShowConfirmPassword((prev) => !prev)}
                                            />
                                        </div>
                                    </div>
                                </>
                            )}
                            
                            {isLogin && (
                                <>
                                    <input
                                        type="email"
                                        className="input_emailid"
                                        placeholder="Enter your Email Id"
                                        name="emailId"
                                        value={logInDetails.emailId}
                                        onChange={handleChange}
                                    />
                                    <br />
                                    <div className="passwordfield">
                                        <input
                                            type={showPassword ? "text" : "password"}
                                            className="input_password"
                                            placeholder="Enter Password"
                                            name="password"
                                            value={logInDetails.password}
                                            onChange={handleChange}
                                        />
                                        <div className="eye-container">
                                            <img
                                                className="eye"
                                                src={showPassword ? "eye.svg" : "eye-closed.svg"}
                                                alt={showPassword ? "Hide password" : "Show password"}
                                                onClick={() => setShowPassword((prev) => !prev)}
                                            />
                                        </div>
                                    </div>
                                </>
                            )}
                        </div>

                        {(isLogin || isSignUp) && (
                            <div className="forgot-password-container">
                                <p className="forgot-password-link">
                                    <span onClick={handleForgotPassword}>Forgot Password?</span>
                                </p>
                            </div>
                        )}

                        <motion.button
                            className="submitButton"
                            type="submit"
                            disabled={isLoading}
                            style={(isForgotPassword || isResetPassword) ? { marginTop: "20px" } : {}}
                            whileHover={{ scale: 1.1 }}
                            whileTap={{ scale: 0.95 }}
                        >
                            {isLoading ? (
                                <LoadingThreeDots />
                            ) : isLogin ? (
                                "Login"
                            ) : isSignUp ? (
                                "Sign Up"
                            ) : isForgotPassword ? (
                                "Reset"
                            ) : isResetPassword ? (
                                "Reset"
                            ) : (
                                "Submit"
                            )}
                        </motion.button>
                        
                        {isSubmitted && message && (
                            <div className="message">
                                <p>{message}</p>
                            </div>
                        )}
                        
                        {!isResetPassword && (
                            <p className="haveaccount">
                                {isLogin
                                    ? "Don't have an account?"
                                    : isSignUp
                                    ? "Already have an account?"
                                    : "Would you like to log in instead?"}
                                <button
                                    className="toggleLogIn"
                                    type="button"
                                    onClick={changeLogin}
                                >
                                    {isLogin ? "Sign up" : "Log in"}
                                </button>
                            </p>
                        )}
                    </form>
                </div>
            </div>
        </>
    );
}

export default AuthPage;
