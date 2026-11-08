import { createRouter, createWebHashHistory } from 'vue-router';

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', redirect: '/employees' },
    {
      path: '/employees',
      name: 'employee-list',
      component: () => import('../views/EmployeeListView.vue'),
    },
    {
      path: '/employees/new',
      name: 'employee-create',
      component: () => import('../views/EmployeeFormView.vue'),
    },
    {
      path: '/employees/:id/edit',
      name: 'employee-edit',
      component: () => import('../views/EmployeeFormView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/employees' },
  ],
});
