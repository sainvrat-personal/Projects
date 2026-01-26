# Email Service Configuration Guide

This application includes a robust email service that allows you to send real emails to customers for various events in the order lifecycle.

## Prerequisites

- A Gmail account (or another email provider, with appropriate configuration changes)
- For Gmail, you'll need to create an "App Password" since regular passwords won't work with SMTP due to Google's security policies

## How to Set Up Gmail App Password

1. Go to your Google Account settings at https://myaccount.google.com/
2. Select "Security" from the left menu
3. Under "Signing in to Google," select "2-Step Verification" and make sure it's enabled
4. Go back to the Security page and select "App passwords" (you may need to sign in again)
5. Select "Mail" as the app and "Other" as the device (name it "Order Management System")
6. Google will generate a 16-character password - copy this password

## Configuration Options

You can configure the email service in two ways:

### Option 1: Environment Variables (Recommended for Production)

Set the following environment variables:

```bash
export EMAIL_USERNAME=your-email@gmail.com
export EMAIL_PASSWORD=your-app-password
```

### Option 2: Update application.properties (For Development Only)

Edit the `src/main/resources/application.properties` file:

```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

> **IMPORTANT**: Never commit actual credentials to source control. The application.properties file should contain placeholder values only.

## Email Features

The system sends emails for the following events:

1. **Order Confirmation**: When a new order is created
2. **Payment Confirmation**: When an order is marked as paid
3. **Shipment Notification**: When an order is shipped (includes invoice as attachment)
4. **Delivery Confirmation**: When an order is marked as delivered
5. **Cancellation Notice**: When an order is cancelled

## Testing the Email Service

The email service includes a fallback logging mechanism if email sending fails. This allows you to see what would have been sent even if the SMTP configuration is not complete.

To test that your email configuration works correctly:

1. Set up the environment variables or application.properties as described above
2. Create a new order with a valid email address
3. Check your email inbox for the order confirmation
4. If you don't receive an email, check the application logs for error messages

## Customizing Email Templates

The email templates are currently defined in the service classes. For a more maintainable solution, consider migrating them to template files using Spring's templating support (Thymeleaf, Freemarker, etc.).

## Troubleshooting

- If you see SSL/TLS errors, make sure your JDK has proper SSL support
- If authentication fails, verify your username and app password
- If using Gmail, ensure that "Less secure app access" is not being relied upon (it's deprecated)
- Check firewall settings if your environment restricts outbound SMTP traffic