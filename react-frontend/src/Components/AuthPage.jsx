import React, {useState} from "react";
import '../Style/AuthPage.css';

function AuthPage(){

    const [signUpDetails, setSignUpDetails] = useState({userName: "",
                                                    emailId: "",
                                                    mobileNumber: "",
                                                    password: ""});

    const [logInDetails, setLogInDetails] = useState({emailId: "",
                                                    password: ""});
    
    
    const [error, setError] = useState(null);    
    const [isLoading, setIsLoading] = useState(false);
    const [isLogin, setIsLogin] = useState(true);
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [showPassword, setShowPassword]= useState(false);

    function changeLogin(){
        setIsLogin((prevIsLogin) => !prevIsLogin)
        setIsSubmitted(false);
        setError(null);
    }

    const handleChange = (e) => {
        if (isLogin) {
            setLogInDetails({ ...logInDetails, [e.target.name]: e.target.value });
        } else {
            setSignUpDetails({ ...signUpDetails, [e.target.name]: e.target.value });
        }
    };

    const validateMobile = (mobileNumber) => {
        return /^\d{10}$/.test(mobileNumber);
    }


    const handleSubmitForm = async (e) => {
        e.preventDefault();
        setIsSubmitted(true);
        setError(null);

        if(isLogin) {
            if (!logInDetails.emailId || !logInDetails.password){
                setError("Please fill all the fields.");
                return;
            }
        }
        else
        {
            if (
                !signUpDetails.userName ||
                !signUpDetails.emailId ||
                !signUpDetails.mobileNumber ||
                !signUpDetails.password
            ){
                setError("Please fill all the fields.");
                return;
            }

            if(!validateMobile(signUpDetails.mobileNumber)){
                setError("Mobile number must be a 10-digit number");
                return;
            }
        }
        setIsLoading(true);
        try
        {
            let response;
            if(isLogin){
                response = await fetch("http://localhost:8080/login",{
                method: "POST",
                headers: {
                    "Content-type": "application/json"
                },
                body: JSON.stringify(logInDetails)
            });
        }

            else{
                response = await fetch("http://localhost:8080/register",{
                    method: "POST",
                    headers: {
                        "Content-type": "application/json"
                    },
                    body: JSON.stringify(signUpDetails)
                });
            }

            const data = await response.json();
            if(response.ok){
                setIsSubmitted(true);
            } else {
                setError(data.message || "Authentication failed.")
            }
        } catch(error){
            setError("Network error or unable to connect to server.");
            console.error("Fetch error",error);
        } finally {
            setIsLoading(false);
        }

    };    

    return(
        <>
            <div className="container">
                <div className="image-container">
                    <div className="frame">
                        <img className="shape" src="shapes.png" alt="Auth page"/>
                    </div>
                </div>
                <div className="Form">
                    {!isLogin ? <h1>Sign Up</h1> : <h1>Log in</h1>}                    
                    <form onSubmit={handleSubmitForm}>
                        {!isLogin && (
                            <>
                                <input 
                                    type="text" 
                                    className="input_username" 
                                    placeholder="Enter your name" 
                                    name="userName"
                                    value={signUpDetails.userName}
                                    onChange={handleChange}
                                />
                                <br/>
                            </>
                        )}

                        <input
                            type="email" 
                            className="input_emailid" 
                            placeholder="Enter your Email Id" 
                            name="emailId"
                            value={isLogin ? logInDetails.emailId : signUpDetails.emailId}
                            onChange={handleChange}
                        />
                        <br/>

                        {!isLogin && (
                            <>
                                <input
                                    type="text" 
                                    className="input_mobilenumber" 
                                    placeholder="Enter your Mobile Number" 
                                    name="mobileNumber"
                                    value={signUpDetails.mobileNumber}
                                    onChange={handleChange}
                                />
                                <br/>
                            </>
                        )}
                        <div className="passwordfield">
                            <input
                                type={showPassword ? "text" : "password"}
                                className="input_password" 
                                placeholder="Enter Password" 
                                name="password"
                                value={isLogin ? logInDetails.password : signUpDetails.password}
                                onChange={handleChange}
                            />
                            <img 
                                className="eye" 
                                src="eye.png" 
                                alt="seePassword" 
                                onClick={() => setShowPassword((prev) => !prev)}/>
                        </div>
                        <br/>

                        <button 
                            className="submitButton" 
                            type="submit" 
                            disabled={isLoading}>
                            {isLoading ? "...Loading" : !isLogin ? "Sign Up" : "Login"}
                        </button>
                        {isSubmitted  && error && <p className="error-message">{error}</p>}
                        {isSubmitted && !isLoading && !error && <p className="success-message">Success!</p> }
                        <p>{!isLogin ? "Already have an account?" : "Don't have an account?"}</p>
                        <button 
                        className="toggleLogIn"
                        type="button"
                        onClick={changeLogin}>
                            {!isLogin ? "Log In" : "Sign up"}
                        </button>
                    </form>                    
                </div>
            </div>
        </>
    );
}

export default AuthPage