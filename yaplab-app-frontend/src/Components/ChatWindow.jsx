import react, {useState, useEffect} from 'react';
import '../Style/ChatWindow.css';


function ChatWindow(){

const [profileImage, setProfileImage] = useState("");

useEffect(() => {
    const profile = Math.floor(Math.random() * 15) + 1;
    setProfileImage(`./avatars/${profile}.png`);    
}, []);
    return(  
        
        <>
            <div className='chatwindow'>
                <div className='Userlistarea'>
                    <p className='chatTitle'>Chats</p>
                    <form className='searchUser'>
                        <input className='searchfield' placeholder='Search'>
                        </input>
                    </form>
                    <br></br>
                    <div className='chatHistory'>
                        <div className='userInfoList'>
                            <img src={profileImage} alt='User Avatar' className='useravatar'></img>
                            Arsh
                        </div>
                    </div>
                </div>

                <div className='chatareacontainer'>
                    <div className='userInfo'>
                        <img src={profileImage} alt='User Avatar' className='useravatar'>
                        </img>
                        <h4>Arsh</h4>
                    </div>
        
                    <div className='chatArea'>
                        <div className='messageContainer'>
                            <form className='getMessage'>
                                <input className='typeArea' placeholder='Write a Message'>
                                </input>
                                <button className='sendMessage'>
                                    <img className='sendIcon' src="sendicon.png"/>
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </>
            
        
    );
}

export default ChatWindow;

