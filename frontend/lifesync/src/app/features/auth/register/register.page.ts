import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';
import { readApiError } from '../../../core/auth/auth-error';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthShell } from '../auth-shell';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password === confirm ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, LucideDynamicIcon, AuthShell],
  templateUrl: './register.page.html',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);

  readonly loading = signal(false);
  readonly showPassword = signal(false);
  readonly showConfirm = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.formBuilder.nonNullable.group(
    {
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      phoneNumber: ['', [Validators.pattern(/^$|^\+?[0-9]{7,15}$/)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatch },
  );

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    const { fullName, email, phoneNumber, password } = this.form.getRawValue();
    const trimmedPhone = phoneNumber.trim();

    this.auth
      .register({
        fullName: fullName.trim(),
        email: email.trim(),
        password,
        ...(trimmedPhone ? { phoneNumber: trimmedPhone } : {}),
      })
      .subscribe({
        next: () => {
          this.auth.login({ email: email.trim(), password }).subscribe({
            next: () => void this.router.navigateByUrl('/dashboard'),
            error: () => {
              void this.router.navigate(['/login'], { queryParams: { registered: '1' } });
            },
          });
        },
        error: (error: unknown) => {
          this.loading.set(false);
          this.errorMessage.set(
            readApiError(error, 'Could not create your account. Please try again.'),
          );
        },
      });
  }
}
