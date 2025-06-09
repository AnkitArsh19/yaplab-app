import React, { useState } from "react";
import "../Style/AuthPage.css";
import { motion } from "framer-motion";
import LoadingThreeDots from "./LoadingThreeDots";
import { useNavigate } from "react-router-dom";

function AuthPage({onLoginSuccess}) {
    const navigate = useNavigate();
    
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

    const [isLoading, setIsLoading] = useState(false);
    const [isLogin, setIsLogin] = useState(true);
    const [isSignUp, setIsSignUp] = useState(false);
    const [isForgotPassword, setIsForgotPassword] = useState(false);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [message, setMessage] = useState("");

    function changeLogin() {
        if (isLogin) {
            setIsLogin(false);
            setIsSignUp(true);
            setIsForgotPassword(false);
            setSignUpDetails({ userName: "", emailId: "", mobileNumber: "", password: "" });
        } else {
            setIsLogin(true);
            setIsSignUp(false);
            setIsForgotPassword(false);
            setLogInDetails({ emailId: "", password: "" });
        }
        setIsSubmitted(false);
        setMessage("");
    }

    const handleForgotPassword = () => {
        setIsLogin(false);
        setIsSignUp(false);
        setIsForgotPassword(true);
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
        } else {
            setForgotPswdDetails((prev) => ({ ...prev, [name]: value }));
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
        } else {
            if (!forgotPswdDetails.emailId) {
                setMessage("Please fill all the fields.");
                return;
            }
        }

        setIsLoading(true);
        try {
            let response;
            if (isLogin) {
                response = await fetch("http://localhost:8080/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(logInDetails),
                });
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    setMessage(errorData.message || "Login failed. Please try again.");
                    return;
                }
                  const data = await response.json();
                onLoginSuccess(data);
                navigate("/chat");
                
            } else if (isSignUp) {
                response = await fetch("http://localhost:8080/auth/register", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(signUpDetails),
                });
                
                const data = await response.json();
                
                if (!response.ok) {
                    setMessage(data.message || "Registration failed. Please try again.");
                    return;
                }
                
                // Success - clear form and show message
                setSignUpDetails({ userName: "", emailId: "", mobileNumber: "", password: "" });
                setMessage(data.message || "Registration successful! Please check your email to verify your account.");
                
            } else {
                // Forgot password
                response = await fetch("http://localhost:8080/auth/forgot-password", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(forgotPswdDetails),
                });
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    setMessage(errorData.message || "Failed to send reset email. Please try again.");
                    return;
                }
                
                setForgotPswdDetails({ emailId: "" });
                setMessage("Password reset link sent to your email!");
            }
        } catch (error) {
            console.error("Error during form submission:", error);
            setMessage("Network error or server unavailable. Please try again later.");
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
                    <div className="logo">
                        <img className="logo-image" src="logo-name.png" alt="logo" />
                    </div>
                    <p className="Heading">
                        {isLogin ? "Login" : isSignUp ? "Sign up" : "Reset Password"}
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
                            style={isForgotPassword ? { marginTop: "20px" } : {}}
                            whileHover={{ scale: 1.1 }}
                            whileTap={{ scale: 0.95 }}
                        >
                            {isLoading ? (
                                <LoadingThreeDots />
                            ) : isLogin ? (
                                "Login"
                            ) : isSignUp ? (
                                "Sign Up"
                            ) : (
                                "Reset"
                            )}
                        </motion.button>
                        
                        {isSubmitted && message && (
                            <div className="message">
                                <p>{message}</p>
                            </div>
                        )}
                        
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
                    </form>
                </div>
            </div>
        </>
    );
}

export default AuthPage;