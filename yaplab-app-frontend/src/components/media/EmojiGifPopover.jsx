import React from 'react';
import '../../styles/EmojiGifPopover.css';

function EmojiGifPopover({ activeTab, setActiveTab, children, ...props }) {
  return (
    <div className="emoji-gif-popover" {...props}>
      <div className="popover-tabs">
        <button
          className={`popover-tab${activeTab === 'emoji' ? ' active' : ''}`}
          onClick={() => setActiveTab('emoji')}
        >
          Emoji
        </button>
        <button
          className={`popover-tab${activeTab === 'gif' ? ' active' : ''}`}
          onClick={() => setActiveTab('gif')}
        >
          GIF
        </button>
      </div>
      <div className="popover-content">
        {children}
      </div>
    </div>
  );
}

export default EmojiGifPopover;
