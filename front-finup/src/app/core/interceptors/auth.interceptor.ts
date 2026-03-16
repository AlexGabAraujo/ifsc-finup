import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from "@angular/common/http";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { catchError, throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  if (req.url.includes('/authentication/actions/login')
    || req.url.includes('/queries/login')
    || req.url.includes('blob-service-storage.s3.amazonaws.com')
  ) {
    return next(req);
  }

  const token = localStorage.getItem('access_token');

  return next(token ? addToken(req, token) : req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        localStorage.clear();
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};

function addToken(req: HttpRequest<unknown>, token: string) {
  return req.clone({
    headers: req.headers.set('Authorization', `Bearer ${token}`)
  });
}

