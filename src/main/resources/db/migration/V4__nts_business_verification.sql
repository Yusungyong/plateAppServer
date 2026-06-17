alter table store_applications
    add column if not exists business_representative_name varchar(100),
    add column if not exists business_opening_date date,
    add column if not exists business_verification_provider varchar(30),
    add column if not exists business_verification_status varchar(30),
    add column if not exists business_verified_at timestamptz,
    add column if not exists business_verification_message varchar(300);
