/**
 * FixLink AI Customer Service Standalone Vanilla JS Widget
 * Single tag embed: <script src="/js/chatbot-widget.js" async></script>
 */
(function() {
  if (window.FixLinkChatbotLoaded) return;
  window.FixLinkChatbotLoaded = true;

  const API_ENDPOINT = '/api/v1/chatbot';
  let sessionId = 'fixlink-vanilla-' + Math.random().toString(36).substring(2, 9) + '-' + Date.now();
  let isOpen = false;
  let isLoading = false;

  // Create floating widget container
  const container = document.createElement('div');
  container.id = 'fixlink-chatbot-root';
  container.style.cssText = 'position:fixed;bottom:20px;right:20px;z-index:999999;font-family:system-ui,-apple-system,sans-serif;';

  // Toggle button
  const button = document.createElement('button');
  button.type = 'button';
  button.innerHTML = `
    <div style="display:flex;align-items:center;gap:8px;">
      <svg style="width:24px;height:24px;fill:none;stroke:currentColor;stroke-width:2;" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"></path>
      </svg>
      <span style="font-weight:600;font-size:14px;">Hỗ trợ FixLink</span>
    </div>
  `;
  button.style.cssText = 'background:linear-gradient(135deg,#2563EB,#1E40AF);color:#fff;border:none;padding:12px 18px;border-radius:9999px;box-shadow:0 10px 25px -5px rgba(37,99,235,0.4);cursor:pointer;display:flex;align-items:center;font-size:14px;transition:all 0.2s ease;';
  button.onmouseover = () => button.style.transform = 'scale(1.05)';
  button.onmouseout = () => button.style.transform = 'scale(1)';

  // Popup window
  const popup = document.createElement('div');
  popup.style.cssText = 'display:none;position:absolute;bottom:65px;right:0;width:380px;max-width:calc(100vw-30px);height:520px;background:#fff;border-radius:16px;box-shadow:0 20px 40px rgba(0,0,0,0.18);border:1px solid #E2E8F0;flex-direction:column;overflow:hidden;animation:fadeIn 0.2s ease-out;';

  // Header
  const header = document.createElement('div');
  header.style.cssText = 'background:linear-gradient(135deg,#2563EB,#1D4ED8);color:#fff;padding:12px 16px;display:flex;align-items:center;justify-content:space-between;';
  header.innerHTML = `
    <div style="display:flex;align-items:center;gap:10px;">
      <div style="width:30px;height:30px;border-radius:50%;background:rgba(255,255,255,0.2);display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:12px;">⚡</div>
      <div>
        <div style="font-weight:700;font-size:14px;line-height:1.2;">Trợ lý FixLink AI</div>
        <div style="font-size:11px;opacity:0.85;display:flex;align-items:center;gap:4px;">
          <span style="width:6px;height:6px;border-radius:50%;background:#34D399;display:inline-block;"></span> Trực tuyến 24/7
        </div>
      </div>
    </div>
    <button id="fixlink-chat-close" style="background:transparent;border:none;color:#fff;font-size:16px;cursor:pointer;padding:4px 8px;border-radius:6px;">✕</button>
  `;

  // Body messages
  const messagesList = document.createElement('div');
  messagesList.style.cssText = 'flex:1;overflow-y:auto;padding:14px;background:#F8FAFC;display:flex;flex-direction:column;gap:10px;font-size:13px;';

  // Suggestions row
  const suggestionsBox = document.createElement('div');
  suggestionsBox.style.cssText = 'padding:6px 10px;background:#fff;border-top:1px solid #F1F5F9;display:flex;gap:6px;overflow-x:auto;white-space:nowrap;';

  // Input area
  const inputForm = document.createElement('form');
  inputForm.style.cssText = 'padding:10px;background:#fff;border-top:1px solid #E2E8F0;display:flex;gap:8px;align-items:center;';
  inputForm.innerHTML = `
    <input type="text" id="fixlink-chat-input" placeholder="Hỏi FixLink về dịch vụ, đặt cọc..." style="flex:1;padding:8px 12px;border:1px solid #CBD5E1;border-radius:10px;font-size:13px;outline:none;" />
    <button type="submit" style="background:#2563EB;color:#fff;border:none;padding:8px 14px;border-radius:10px;font-weight:600;font-size:13px;cursor:pointer;">Gửi</button>
  `;

  popup.appendChild(header);
  popup.appendChild(messagesList);
  popup.appendChild(suggestionsBox);
  popup.appendChild(inputForm);

  container.appendChild(button);
  container.appendChild(popup);
  document.body.appendChild(container);

  // Helper: Append message
  function appendMessage(sender, text) {
    const bubble = document.createElement('div');
    if (sender === 'user') {
      bubble.style.cssText = 'align-self:flex-end;max-width:80%;background:#2563EB;color:#fff;padding:8px 12px;border-radius:14px 14px 2px 14px;line-height:1.4;word-break:break-word;';
      bubble.textContent = text;
    } else {
      bubble.style.cssText = 'align-self:flex-start;max-width:85%;background:#fff;color:#1E293B;padding:10px 14px;border-radius:14px 14px 14px 2px;line-height:1.5;border:1px solid #E2E8F0;box-shadow:0 1px 2px rgba(0,0,0,0.05);word-break:break-word;';
      bubble.innerHTML = text.replace(/\n/g, '<br/>').replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    }
    messagesList.appendChild(bubble);
    messagesList.scrollTop = messagesList.scrollHeight;
  }

  // Helper: Render suggestions
  function renderSuggestions(list) {
    suggestionsBox.innerHTML = '';
    (list || []).forEach(item => {
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.textContent = '💡 ' + item;
      chip.style.cssText = 'background:#EFF6FF;color:#1D4ED8;border:1px solid #BFDBFE;padding:4px 10px;border-radius:9999px;font-size:11px;cursor:pointer;flex-shrink:0;';
      chip.onclick = () => doSendMessage(item);
      suggestionsBox.appendChild(chip);
    });
  }

  async function doSendMessage(text) {
    if (!text || !text.trim() || isLoading) return;
    const msg = text.trim();
    appendMessage('user', msg);
    isLoading = true;

    const loadingBubble = document.createElement('div');
    loadingBubble.style.cssText = 'align-self:flex-start;color:#64748B;font-size:12px;font-style:italic;';
    loadingBubble.textContent = 'FixLink đang trả lời...';
    messagesList.appendChild(loadingBubble);
    messagesList.scrollTop = messagesList.scrollHeight;

    try {
      const res = await fetch(`${API_ENDPOINT}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sessionId: sessionId,
          message: msg,
          route: window.location.pathname
        })
      });
      const data = await res.json();
      messagesList.removeChild(loadingBubble);
      appendMessage('bot', data.reply || 'Xin lỗi, tôi chưa hiểu rõ.');
      if (data.suggestions && data.suggestions.length > 0) {
        renderSuggestions(data.suggestions);
      }
    } catch (err) {
      if (loadingBubble.parentNode) messagesList.removeChild(loadingBubble);
      appendMessage('bot', '⚠️ Không thể kết nối tới máy chủ. Vui lòng thử lại sau.');
    } finally {
      isLoading = false;
    }
  }

  // Event handlers
  button.onclick = () => {
    isOpen = !isOpen;
    popup.style.display = isOpen ? 'flex' : 'none';
    if (isOpen && messagesList.children.length === 0) {
      appendMessage('bot', '👋 **Xin chào!** Tôi là trợ lý FixLink. Bạn cần hỗ trợ gì về đặt thợ, quy trình hay sự cố?');
      renderSuggestions([
        'Cách đăng yêu cầu sửa chữa?',
        'Phí dịch vụ và đặt cọc?',
        'Làm thế nào để đăng ký làm thợ?'
      ]);
    }
  };

  header.querySelector('#fixlink-chat-close').onclick = () => {
    isOpen = false;
    popup.style.display = 'none';
  };

  inputForm.onsubmit = (e) => {
    e.preventDefault();
    const input = document.getElementById('fixlink-chat-input');
    if (input && input.value) {
      const val = input.value;
      input.value = '';
      doSendMessage(val);
    }
  };
})();
