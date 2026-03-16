import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: '' ,
        loadComponent: () => import('./shared/layout/layout').then(m => m.Layout),
        children: [
            {
                path: 'dashboard',
                loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard),
                data: { title: 'Dashboard' }
            },
            {
                path: 'categoria',
                loadComponent: () => import('./features/categoria/categoria').then(m => m.Categoria),
                data: { title: 'Categorias' }
            },
            {
                path: '',
                redirectTo: 'dashboard',
                pathMatch: 'full'
            }
        ]
    },
    {
        path: '**',
        redirectTo: 'dashboard'
    }
];
