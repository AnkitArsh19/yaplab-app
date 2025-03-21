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
    const [isLogin, setIsLogin] = useState(true);
    const [isSubmitted, setIsSubmitted] = useState(false);

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

    const handleSubmitForm = async (e) => {
        e.preventDefault();
        setIsSubmitted(false);
        setError(null);
        try{
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
            }
        } catch(error){
            setError("Network error");
            console.error(error);
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
                                    required
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
                            required
                            value={isLogin ? logInDetails.emailId : signUpDetails.emailId}
                            onChange={handleChange}
                        />
                        <br/>

                        {!isLogin && (
                            <>
                                <input
                                    type="number" 
                                    className="input_mobilenumber" 
                                    placeholder="Enter your Mobile Number" 
                                    name="mobileNumber"
                                    value={signUpDetails.mobileNumber}
                                    onChange={handleChange}
                                    required
                                />
                                <br/>
                            </>
                        )}

                        <input
                            type="password" 
                            className="input_password" 
                            placeholder="Enter Password" 
                            required
                            name="password"
                            value={isLogin ? logInDetails.password : signUpDetails.password}
                            onChange={handleChange}
                        />
                        <br/>

                        <button className="submitButton" type="submit" onClick={handleSubmitForm}>
                            {!isLogin ? "Sign Up" : "Login"}
                        </button>
                        {error && <p>{error}</p>}
                        <p>{!isLogin ? "Already have an account?" : "Don't have an account?"}</p>
                        <button className="toggleLogIn" onClick={changeLogin}>
                            {!isLogin ? "Log In" : "Sign up"}
                        </button>
                    </form>                    
                </div>
            </div>
        </>
    );
}

export default AuthPage