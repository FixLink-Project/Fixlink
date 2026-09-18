import './style.css';
import { fetchCategories } from './api/apiClient.js';

document.addEventListener('DOMContentLoaded', async () => {
  const categoryGrid = document.getElementById('categoryGrid');
  if (!categoryGrid) return;

  try {
    const categories = await fetchCategories();
    if (categories && categories.length > 0) {
      const iconMap = {
        'cat_1': '⚡',
        'cat_2': '🚰',
        'cat_3': '❄️',
        'cat_4': '🎨',
        'cat_5': '📱'
      };

      categoryGrid.innerHTML = categories.map(cat => `
        <div class="category-card" data-id="${cat.id}">
          <span class="cat-icon">${iconMap[cat.id] || '🛠️'}</span>
          <h3>${cat.name}</h3>
          <p>${cat.description || 'Dịch vụ sửa chữa chuyên nghiệp tận nơi'}</p>
        </div>
      `).join('');
    }
  } catch (err) {
    console.error('Failed to load categories', err);
  }
});
