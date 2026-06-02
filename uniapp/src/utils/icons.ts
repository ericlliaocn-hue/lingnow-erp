const svgHeader = 'data:image/svg+xml;charset=utf-8,';

export const COLORS = {
    primary: '#FF6F61',
    text: '#333333',
    inactive: '#aaaaaa',
    nav_clock: '#FF8A65'
};

const getSvg = (path: string, color: string = COLORS.text) => {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="${color}"><path d="${path}"/></svg>`;
    return svgHeader + encodeURIComponent(svg);
};

const paths = {
    home: "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z",
    category: "M4 11h5V5H4v6zm0 7h5v-6H4v6zm6 0h5v-6h-5v6zm6 0h5v-6h-5v6zm-6-7h5V5h-5v6zm6-6v6h5V5h-5z",
    product: "M20 6h-8.18C11.4 4.84 10.3 4 9 4S6.6 4.84 6.18 6H4c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zM9 6c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm11 12H4V8h2.18C6.6 9.16 7.7 10 9 10s2.4-.84 2.82-2H20v10z",
    order: "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
    mine: "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
    phone: "M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 1.23 0 2.44.2 3.57.57.35.13.74.04 1.02-.24l2.2-2.2z",
    lock: "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3 3.1-3s3.1 1.29 3.1 3v2z",
    message: "M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z",
    arrowBack: "M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z"
};

export const icons = {
    home: getSvg(paths.home, COLORS.inactive),
    homeActive: getSvg(paths.home, COLORS.primary),
    category: getSvg(paths.category, COLORS.inactive),
    categoryActive: getSvg(paths.category, COLORS.primary),
    product: getSvg(paths.product, COLORS.inactive),
    productActive: getSvg(paths.product, COLORS.primary),
    order: getSvg(paths.order, COLORS.inactive),
    orderActive: getSvg(paths.order, COLORS.primary),
    mine: getSvg(paths.mine, COLORS.inactive),
    mineActive: getSvg(paths.mine, COLORS.primary),
    phone: getSvg(paths.phone, COLORS.nav_clock),
    lock: getSvg(paths.lock, COLORS.nav_clock),
    code: getSvg(paths.message, COLORS.nav_clock),
    arrowBack: getSvg(paths.arrowBack, COLORS.text)
};
