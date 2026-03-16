import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [
    {
        path: 'login',
        loadComponent: () =>
            import('./auth/pages/login/login.component').then((m) => m.LoginComponent),
    },
    {
        path: 'cadastro',
        loadComponent: () =>
            import('./auth/pages/cadastro/cadastro').then((m) => m.CadastroComponent),
    },

    {
        path: '',
        loadComponent: () =>
            import('./shared/layout/layout').then((m) => m.Layout),
        children: [
            { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

            {
                path: 'dashboard',
                canActivate: [authGuard],
                loadComponent: () =>
                    import('./features/dashboard/dashboard').then((m) => m.Dashboard),
                data: { title: 'Dashboard' }
            },
            {
                path: 'categoria',
                canActivate: [authGuard],
                loadComponent: () =>
                    import('./features/categoria/categoria').then((m) => m.Categoria),
                data: { title: 'Categorias' }
            }
        ]
    },
    {
        path: '**',
        loadComponent: () =>
            import('./features/not-found/not-found').then((m) => m.NotFound),
    },
];