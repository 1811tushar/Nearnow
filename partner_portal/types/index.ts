export type Role='admin'|'warehouse_manager'|'vendor';
export type Product={id:number;name:string;description?:string;categoryId:number;images?:string[];price:number;salePrice:number;effectivePrice?:number;discountPercent?:number;unit?:string;stock:number;rating:number;isFeatured:boolean;barcode?:string;reviewCount:number;active:boolean};
export type Page<T>={content:T[];totalElements:number;totalPages:number;page:number;size:number;hasMore:boolean};
export type PickItem={id:number;productId:number;productName:string;barcode:string;quantity:number;picked:boolean};
export type PickList={id:number;orderId:number;storeId:number;status:string;items:PickItem[]};
export type Stock={id:number;storeId:number;productId:number;productName:string;barcode:string;quantity:number};
export type VendorProduct={productId:number;name:string;barcode:string;price:number;salePrice:number;stock:number;warehouseManaged:boolean;active:boolean};
export type RestockRequest={id:number;productId:number;productName:string;quantity:number;note?:string;status:'PENDING'|'APPROVED'|'REJECTED';createdAt:string};
