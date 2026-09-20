import { forwardRef } from 'react';
import { Link, NavLink } from 'react-router-dom';
import type { LinkProps, NavLinkProps } from 'react-router-dom';

export const RouterLinkBehavior = forwardRef<HTMLAnchorElement, LinkProps>(function RouterLinkBehavior(props, ref) {
  return <Link ref={ref} {...props} />;
});

export const RouterNavLinkBehavior = forwardRef<HTMLAnchorElement, NavLinkProps>(function RouterNavLinkBehavior(props, ref) {
  return <NavLink ref={ref} {...props} />;
});
